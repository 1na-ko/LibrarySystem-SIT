package com.library.kg.service.impl;

import com.library.core.entity.Book;
import com.library.core.mapper.BookMapper;
import com.library.core.vo.BookSimpleVO;
import com.library.kg.config.KnowledgeGraphProperties;
import com.library.kg.enums.GraphNodeType;
import com.library.kg.enums.GraphRelationType;
import com.library.kg.enums.TraceDirection;
import com.library.kg.model.GraphEdge;
import com.library.kg.model.GraphNode;
import com.library.kg.model.TracePath;
import com.library.kg.repository.Neo4jRepository;
import com.library.kg.service.LiteratureTracingService;
import com.library.kg.vo.TraceGraphVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 文献溯源服务实现.
 * <p>
 * 通过引用链（CITES）BFS 多跳遍历构建文献演变关系图。
 * 关键路径发现优先使用 GDS Dijkstra，不可用时降级 Cypher {@code shortestPath()}.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LiteratureTracingServiceImpl implements LiteratureTracingService {

    private final Neo4jRepository neo4jRepository;
    private final BookMapper bookMapper;
    private final KnowledgeGraphProperties kgProperties;

    private static final String CITE_REL = "CITES";
    private static final int TRACE_LIMIT = 200;

    @Override
    public TraceGraphVO trace(Long bookId, TraceDirection direction, int maxDepth) {
        int safeDepth = clamp(maxDepth, 1, kgProperties.getTracingMaxDepth());
        BookSimpleVO sourceBook = loadBookVO(bookId);
        if (sourceBook == null) {
            return TraceGraphVO.builder().sourceBook(null).paths(List.of()).build();
        }

        String arrow = switch (direction) {
            case FORWARD -> "-[:" + CITE_REL + "*1.." + safeDepth + "]->";
            case BACKWARD -> "<-[:" + CITE_REL + "*1.." + safeDepth + "]-";
            case BOTH -> "-[:" + CITE_REL + "*1.." + safeDepth + "]-";
        };

        String cypher = "MATCH path = (start:Book {id: $bookId})" + arrow + "(target:Book) "
                + "RETURN path LIMIT " + kgProperties.getTracingLimit();

        List<List<Object>> rawPaths = neo4jRepository.query(cypher,
                Map.of("bookId", bookId),
                (rec) -> rec.get("path").asList());

        List<TracePath> paths = parseTracePaths(rawPaths);

        return TraceGraphVO.builder()
                .sourceBook(sourceBook)
                .paths(paths)
                .build();
    }

    @Override
    public TraceGraphVO findKeyPath(Long fromBookId, Long toBookId) {
        BookSimpleVO sourceBook = loadBookVO(fromBookId);
        if (sourceBook == null) return null;

        List<Long> nodeIds = neo4jRepository.shortestPath(fromBookId, toBookId, CITE_REL);
        if (nodeIds.isEmpty()) return null;

        // 批量加载所有图书节点——消除 N+1 逐书查询
        List<GraphNode> pathNodes = buildBookNodes(nodeIds);
        List<GraphEdge> pathEdges = new ArrayList<>();
        for (int i = 1; i < nodeIds.size(); i++) {
            pathEdges.add(GraphEdge.builder()
                    .sourceId(nodeIds.get(i - 1))
                    .targetId(nodeIds.get(i))
                    .relation(GraphRelationType.CITES)
                    .weight(1.0)
                    .build());
        }

        TracePath keyPath = TracePath.builder()
                .nodes(pathNodes)
                .edges(pathEdges)
                .depth(pathNodes.size() - 1)
                .totalWeight(pathEdges.stream().mapToDouble(GraphEdge::getWeight).sum())
                .build();

        return TraceGraphVO.builder()
                .sourceBook(sourceBook)
                .paths(List.of(keyPath))
                .build();
    }

    // ---- 内部 ----

    /**
     * 获取图书的 BookSimpleVO（从 MySQL）.
     */
    private BookSimpleVO loadBookVO(Long bookId) {
        Book book = bookMapper.selectById(bookId);
        if (book == null || (book.getDeleted() != null && book.getDeleted() == 1)) return null;
        return BookSimpleVO.builder()
                .id(book.getId())
                .isbn(book.getIsbn())
                .title(book.getTitle())
                .author(book.getAuthor())
                .publisher(book.getPublisher())
                .coverUrl(book.getCoverUrl())
                .build();
    }

    /**
     * 解析 Neo4j 路径数据为 TracePath 列表.
     * <p>
     * Neo4j 5.x Driver 中路径段交替排列为 NODE / RELATIONSHIP / NODE / ...。
     * 遍历时从前后的 NODE 段提取业务 {@code id} 作为 CITES 边的 sourceId / targetId。
     */
    private List<TracePath> parseTracePaths(List<List<Object>> rawPaths) {
        List<TracePath> paths = new ArrayList<>();
        for (List<Object> pathSegments : rawPaths) {
            if (pathSegments == null || pathSegments.isEmpty()) continue;
            // 预过滤：只保留 Value 类型段
            List<Value> segments = new ArrayList<>();
            for (Object seg : pathSegments) {
                if (seg instanceof Value v) segments.add(v);
            }
            if (segments.isEmpty()) continue;

            // 第一遍：收集节点
            List<GraphNode> nodes = new ArrayList<>();
            for (Value segVal : segments) {
                if ("NODE".equals(segVal.type().name())) {
                    try {
                        nodes.add(buildBookNodeFromValue(segVal));
                    } catch (Exception e) {
                        log.debug("溯源节点段解析失败（预期内，跳过）: {}", e.getMessage());
                    }
                }
            }

            // 第二遍：构建边——使用相邻节点的业务 ID
            List<GraphEdge> edges = new ArrayList<>();
            for (int i = 1; i < segments.size() - 1; i++) {
                Value segVal = segments.get(i);
                if ("RELATIONSHIP".equals(segVal.type().name())) {
                    try {
                        // 前一个 NODE 为 source，后一个 NODE 为 target
                        Long srcId = extractNodeBizId(segments, i - 1);
                        Long tgtId = extractNodeBizId(segments, i + 1);
                        if (srcId != null && tgtId != null) {
                            edges.add(GraphEdge.builder()
                                    .sourceId(srcId)
                                    .targetId(tgtId)
                                    .relation(GraphRelationType.CITES)
                                    .weight(1.0)
                                    .build());
                        }
                    } catch (Exception e) {
                        log.debug("溯源边段解析失败（预期内，跳过）: {}", e.getMessage());
                    }
                }
            }

            if (!nodes.isEmpty()) {
                paths.add(TracePath.builder()
                        .nodes(nodes)
                        .edges(edges)
                        .depth(nodes.size() - 1)
                        .totalWeight(edges.stream().mapToDouble(e -> e.getWeight() != null ? e.getWeight() : 1.0).sum())
                        .build());
            }
        }
        return paths;
    }

    /**
     * 从路径段列表中提取 NODE 位置的业务 {@code id} 属性.
     */
    private Long extractNodeBizId(List<Value> segments, int pos) {
        if (pos < 0 || pos >= segments.size()) return null;
        Value segVal = segments.get(pos);
        if (!"NODE".equals(segVal.type().name())) return null;
        try {
            Map<String, Object> props = segVal.asMap();
            if (props.containsKey("id") && props.get("id") instanceof Number) {
                return ((Number) props.get("id")).longValue();
            }
        } catch (Exception e) {
            log.debug("提取节点业务 ID 失败: pos={}, error={}", pos, e.getMessage());
        }
        return null;
    }

    /**
     * 批量加载图书节点——消除 N+1 逐书查询.
     */
    private List<GraphNode> buildBookNodes(List<Long> bookIds) {
        if (bookIds.isEmpty()) return List.of();
        List<Book> books = bookMapper.selectBatchIds(bookIds);
        Map<Long, Book> bookMap = new java.util.HashMap<>();
        for (Book b : books) {
            if (b.getDeleted() == null || b.getDeleted() != 1) {
                bookMap.put(b.getId(), b);
            }
        }
        return bookIds.stream()
                .map(id -> {
                    Book book = bookMap.get(id);
                    return GraphNode.builder()
                            .id(id)
                            .label(book != null ? book.getTitle() : "Book#" + id)
                            .type(GraphNodeType.BOOK)
                            .build();
                })
                .toList();
    }

    private GraphNode buildBookNodeFromValue(Value nodeVal) {
        Map<String, Object> props = nodeVal.asMap();
        long entityId = 0;
        if (props.containsKey("id") && props.get("id") instanceof Number) {
            entityId = ((Number) props.get("id")).longValue();
        }
        String label = props.containsKey("title") ? props.get("title").toString() : "Book";
        return GraphNode.builder()
                .id(entityId)
                .label(label)
                .type(GraphNodeType.BOOK)
                .build();
    }

    private int clamp(int value, int min, int max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }
}
