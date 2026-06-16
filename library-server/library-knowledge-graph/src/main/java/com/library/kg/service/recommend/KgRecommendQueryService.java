package com.library.kg.service.recommend;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.core.entity.BorrowRecord;
import com.library.core.mapper.BorrowRecordMapper;
import com.library.core.service.KgRecommendPort;
import com.library.kg.repository.Neo4jRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * KG 多跳推荐查询服务.
 * <p>
 * 基于用户借阅图书在 Neo4j 中的 1-2 跳邻居（CITES/HAS_KEYWORD/AUTHORED_BY）+
 * PageRank 加权排序，生成 KG 路推荐候选。
 * 仅在 Neo4jClient Bean 可用时注入（{@code @ConditionalOnBean}），
 * Neo4j 不可用时此 Bean 不存在 → 适配器静默降级空 Map。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@Service
@ConditionalOnBean(Neo4jClient.class)
public class KgRecommendQueryService implements KgRecommendPort {

    private final Neo4jRepository neo4jRepository;
    private final BorrowRecordMapper borrowRecordMapper;

    public KgRecommendQueryService(Neo4jRepository neo4jRepository,
                                   BorrowRecordMapper borrowRecordMapper) {
        this.neo4jRepository = neo4jRepository;
        this.borrowRecordMapper = borrowRecordMapper;
    }

    @Override
    public Map<Long, Double> recommend(Long userId, int topN) {
        List<Long> seedIds = getUserBorrowedBookIds(userId);
        if (seedIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // 使用参数绑定传递 seed 列表，避免 Cypher 注入
        int safeTopN = Math.min(topN, 50);
        String cypher = "UNWIND $seeds AS seedId "
                + "MATCH (seed:Book {id: seedId})-[*1..2]-(neighbor:Book) "
                + "WHERE NOT neighbor.id IN $seeds "
                + "RETURN neighbor.id AS bookId, "
                + "coalesce(neighbor.pagerank, 0.0) AS pagerank, "
                + "count(DISTINCT seed) AS pathCount "
                + "ORDER BY pagerank DESC, pathCount DESC "
                + "LIMIT " + safeTopN;

        List<Map<String, Object>> results = neo4jRepository.query(cypher,
                Map.of("seeds", seedIds, "topN", safeTopN * 2),
                (rec) -> Map.of(
                        "bookId", (Object) rec.get("bookId").asLong(),
                        "pagerank", (Object) rec.get("pagerank").asDouble(),
                        "pathCount", (Object) rec.get("pathCount").asLong()));

        if (results.isEmpty()) {
            return Collections.emptyMap();
        }

        // 归一化分数到 [0, 1]
        double maxPageRank = results.stream()
                .mapToDouble(r -> (Double) r.get("pagerank"))
                .max().orElse(1.0);
        Map<Long, Double> scores = new HashMap<>();
        for (var row : results) {
            Long bookId = (Long) row.get("bookId");
            double pr = (Double) row.get("pagerank");
            double score = maxPageRank > 0 ? pr / maxPageRank : 0.0;
            scores.put(bookId, Math.min(score, 1.0));
            if (scores.size() >= safeTopN) break;
        }
        return scores;
    }

    private List<Long> getUserBorrowedBookIds(Long userId) {
        List<BorrowRecord> records = borrowRecordMapper.selectList(
                new LambdaQueryWrapper<BorrowRecord>()
                        .eq(BorrowRecord::getUserId, userId)
                        .eq(BorrowRecord::getDeleted, 0)
                        .select(BorrowRecord::getBookId));
        return records.stream()
                .map(BorrowRecord::getBookId)
                .distinct()
                .toList();
    }
}
