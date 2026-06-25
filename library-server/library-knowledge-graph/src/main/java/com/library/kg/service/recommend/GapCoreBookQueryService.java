package com.library.kg.service.recommend;

import com.library.core.entity.Category;
import com.library.core.mapper.CategoryMapper;
import com.library.core.service.GapCoreBookPort;
import com.library.kg.repository.Neo4jRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 学科核心书目查询服务.
 * <p>
 * 从 Neo4j 中查询指定学科（Subject / category）下 PageRank 最高的 Top-N 图书 ID。
 * 供 {@code library-acquisition} 模块的缺口分析服务消费。
 * 仅在 Neo4jClient Bean 可用时注入。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@Service
@ConditionalOnBean(Neo4jClient.class)
public class GapCoreBookQueryService implements GapCoreBookPort {

    private final Neo4jRepository neo4jRepository;
    private final CategoryMapper categoryMapper;

    public GapCoreBookQueryService(Neo4jRepository neo4jRepository,
                                   CategoryMapper categoryMapper) {
        this.neo4jRepository = neo4jRepository;
        this.categoryMapper = categoryMapper;
    }

    @Override
    public List<Long> getCoreBookIds(Long subjectId, int topN) {
        int safeTopN = Math.min(topN, 100);

        // 优先：通过 Book 节点的 categoryId 属性直接匹配
        List<Long> bookIds = neo4jRepository.query("""
                MATCH (b:Book)
                WHERE b.categoryId = $subjectId
                RETURN b.id AS bookId
                ORDER BY coalesce(b.pagerank, 0.0) DESC
                LIMIT $topN
                """,
                Map.of("subjectId", subjectId, "topN", safeTopN),
                (rec) -> rec.get("bookId").asLong());

        if (!bookIds.isEmpty()) {
            return bookIds;
        }

        // 回退：通过 BELONGS_TO 关系查询（subjectId → MySQL category.name → Neo4j Subject.name）
        Category category = categoryMapper.selectById(subjectId);
        if (category == null) {
            return Collections.emptyList();
        }

        return neo4jRepository.query("""
                MATCH (s:Subject {name: $subjectName})<-[:BELONGS_TO]-(b:Book)
                RETURN b.id AS bookId
                ORDER BY coalesce(b.pagerank, 0.0) DESC
                LIMIT $topN
                """,
                Map.of("subjectName", category.getName(), "topN", safeTopN),
                (rec) -> rec.get("bookId").asLong());
    }
}
