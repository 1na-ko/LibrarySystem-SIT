package com.library.kg.service.recommend;

import com.library.core.entity.Book;
import com.library.core.entity.Category;
import com.library.core.mapper.BookMapper;
import com.library.core.mapper.CategoryMapper;
import com.library.core.service.KgRelatedBookPort;
import com.library.core.vo.BookRecommendVO;
import com.library.core.vo.BookSimpleVO;
import com.library.kg.repository.Neo4jRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * KG 相关图书查询服务.
 * <p>
 * 通过 Neo4j 1 跳邻居（HAS_KEYWORD/AUTHORED_BY/BELONGS_TO）查询与指定图书相关的图书，
 * 按邻居 PageRank 排序。
 * 仅在 Neo4jClient Bean 可用时注入。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(Neo4jClient.class)
public class KgRelatedBookQueryService implements KgRelatedBookPort {

    private final Neo4jRepository neo4jRepository;
    private final BookMapper bookMapper;
    private final CategoryMapper categoryMapper;

    @Override
    public List<BookRecommendVO> getRelated(Long bookId, int limit) {
        int safeLimit = Math.min(limit, 20);

        // Cypher: 从目标书出发 1 跳邻居 Book 节点，按 PageRank 排序
        String cypher = """
                MATCH (b:Book {id: $bookId})-[*1]-(neighbor:Book)
                WHERE neighbor.id <> $bookId
                RETURN DISTINCT neighbor.id AS bookId,
                       coalesce(neighbor.pagerank, 0.0) AS pagerank
                ORDER BY pagerank DESC
                LIMIT $limit
                """;

        List<Map<String, Object>> results = neo4jRepository.query(cypher,
                Map.of("bookId", bookId, "limit", safeLimit * 2),
                (rec) -> Map.of(
                        "bookId", (Object) rec.get("bookId").asLong(),
                        "pagerank", (Object) rec.get("pagerank").asDouble()));

        if (results.isEmpty()) {
            return Collections.emptyList();
        }

        // 按 PageRank 归一化为 0-1 分数
        double maxPageRank = results.stream()
                .mapToDouble(r -> (Double) r.get("pagerank"))
                .max().orElse(1.0);

        // 批量加载图书 + 分类，消除 N+1
        List<Long> neighborIds = results.stream()
                .map(r -> (Long) r.get("bookId")).toList();
        java.util.Map<Long, Book> bookMap = bookMapper.selectBatchIds(neighborIds).stream()
                .collect(java.util.stream.Collectors.toMap(Book::getId, b -> b, (a, b) -> a));
        java.util.Set<Long> catIds = bookMap.values().stream()
                .map(Book::getCategoryId).filter(id -> id != null).collect(java.util.stream.Collectors.toSet());
        java.util.Map<Long, String> catNameMap = catIds.isEmpty() ? java.util.Map.of()
                : categoryMapper.selectBatchIds(catIds).stream()
                .filter(c -> c != null)
                .collect(java.util.stream.Collectors.toMap(
                        com.library.core.entity.Category::getId,
                        com.library.core.entity.Category::getName,
                        (a, b) -> a));

        List<BookRecommendVO> vos = new ArrayList<>();
        for (var row : results) {
            Long nbId = (Long) row.get("bookId");
            double pr = (Double) row.get("pagerank");
            double score = maxPageRank > 0 ? Math.min(pr / maxPageRank * 0.7 + 0.3, 1.0) : 0.5;

            Book book = bookMap.get(nbId);
            if (book == null || (book.getDeleted() != null && book.getDeleted() == 1)) continue;
            String catName = book.getCategoryId() != null ? catNameMap.get(book.getCategoryId()) : null;
            vos.add(BookRecommendVO.builder()
                    .book(BookSimpleVO.builder()
                            .id(book.getId())
                            .isbn(book.getIsbn())
                            .title(book.getTitle())
                            .author(book.getAuthor())
                            .publisher(book.getPublisher())
                            .coverUrl(book.getCoverUrl())
                            .pubDate(book.getPubDate())
                            .availCopies(book.getAvailCopies())
                            .categoryName(catName)
                            .build())
                    .score(score)
                    .reason("知识图谱关联图书")
                    .build());
            if (vos.size() >= safeLimit) break;
        }

        return vos;
    }
}
