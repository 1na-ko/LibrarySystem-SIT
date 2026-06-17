package com.library.kg.service.impl;

import com.library.kg.config.KnowledgeGraphProperties;
import com.library.kg.enums.GraphNodeType;
import com.library.kg.enums.GraphRelationType;
import com.library.kg.model.GraphEdge;
import com.library.kg.model.GraphNode;
import com.library.kg.repository.Neo4jRepository;
import com.library.kg.service.GraphQueryService;
import com.library.kg.vo.KnowledgeGraphVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识图谱查询服务实现.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GraphQueryServiceImpl implements GraphQueryService {

    private final Neo4jRepository neo4jRepository;
    private final KnowledgeGraphProperties kgProperties;

    private static final String BOOK_LABEL = "Book";
    /** 允许的节点类型白名单（防 Cypher 注入） */
    private static final java.util.Set<String> ALLOWED_NODE_TYPES =
            java.util.Set.of("Book", "Author", "Keyword", "Subject", "Publication", "Conference");

    @Override
    public KnowledgeGraphVO getBookGraph(Long bookId, int depth) {
        int safeDepth = clamp(depth, 1, kgProperties.getMaxQueryDepth());

        // 路径查询：以书为中心，取 depth 跳内邻居
        // 注意：可变长度深度参数无法用 $param 绑定，使用整数白名单校验后字符串拼接
        String cypher = "MATCH p = (b:Book {id: $bookId})-[*1.." + safeDepth + "]-(n) "
                + "RETURN p LIMIT 200";
        List<List<Object>> paths = neo4jRepository.query(cypher,
                Map.of("bookId", bookId),
                (rec) -> rec.get("p").asList());

        return buildGraphFromPaths(bookId, paths);
    }

    @Override
    public KnowledgeGraphVO searchEntities(String entity, String type) {
        StringBuilder cypher = new StringBuilder();
        Map<String, Object> params = new HashMap<>();
        params.put("entity", entity);

        if (type != null && !type.isEmpty()) {
            // 白名单校验，防 Cypher 注入
            if (!ALLOWED_NODE_TYPES.contains(type)) {
                log.warn("非法的实体类型参数: type={}, 已拒绝", type);
                return KnowledgeGraphVO.builder().nodes(List.of()).edges(List.of()).build();
            }
            cypher.append("MATCH (n:").append(type).append(") ")
                    .append("WHERE n.name CONTAINS $entity OR n.title CONTAINS $entity ")
                    .append("RETURN n ORDER BY coalesce(n.pagerank, 0.0) DESC LIMIT 50");
        } else {
            // 模糊搜索所有类型节点
            cypher.append("MATCH (n) ")
                    .append("WHERE (n:Book OR n:Author OR n:Keyword OR n:Subject) ")
                    .append("AND (n.name CONTAINS $entity OR n.title CONTAINS $entity) ")
                    .append("RETURN n ORDER BY coalesce(n.pagerank, 0.0) DESC LIMIT 50");
        }

        List<GraphNode> nodes = neo4jRepository.query(cypher.toString(), params,
                (rec) -> {
                    var node = rec.get("n");
                    Map<String, Object> props = node.asMap();
                    return buildNodeFromValue(node);
                });

        return KnowledgeGraphVO.builder()
                .nodes(nodes)
                .edges(List.of())
                .build();
    }

    // ---- 内部 ----

    /**
     * 从路径列表构建图谱 VO：提取去重节点 + 边.
     * <p>
     * Neo4j 5.x Driver 中 path 的 Value.asList() 返回交替的 NODE / RELATIONSHIP 段：
     * <pre>{@code
     *   [NODE(start), RELATIONSHIP, NODE, RELATIONSHIP, ..., NODE(end)]
     * }</pre>
     * 遍历时通过 {@code segVal.type().name()} 识别段类型，并为每条 RELATIONSHIP
     * 从前后的 NODE 段中提取业务 {@code id} 属性作为边的 sourceId / targetId.
     */
    private KnowledgeGraphVO buildGraphFromPaths(Long centerBookId, List<List<Object>> paths) {
        Map<Long, GraphNode> nodeMap = new LinkedHashMap<>();
        // key = sourceId + ":" + targetId + ":" + relationType → 去重用
        Map<String, GraphEdge> edgeMap = new LinkedHashMap<>();

        for (List<Object> path : paths) {
            if (path == null || path.isEmpty()) continue;
            // 第一遍：收集所有节点，同时记录每个位置对应的节点业务 ID
            List<Value> segmentValues = new ArrayList<>();
            for (Object seg : path) {
                if (seg instanceof Value v) segmentValues.add(v);
            }
            if (segmentValues.isEmpty()) continue;

            // 预提取每个 NODE 位置的业务 ID（用于后续 RELATIONSHIP 连接）
            Long[] prevNodeId = new Long[segmentValues.size()];
            Long[] nextNodeId = new Long[segmentValues.size()];

            for (int i = 0; i < segmentValues.size(); i++) {
                Value segVal = segmentValues.get(i);
                if ("NODE".equals(segVal.type().name())) {
                    GraphNode gn = buildNodeFromValue(segVal);
                    nodeMap.putIfAbsent(gn.getId(), gn);
                    // 向前查找：上一个 NODE 的 i 位置记录此节点为 "next"
                    for (int j = i - 1; j >= 0; j--) {
                        if ("RELATIONSHIP".equals(segmentValues.get(j).type().name()) && nextNodeId[j] == null) {
                            nextNodeId[j] = gn.getId();
                        }
                        if ("NODE".equals(segmentValues.get(j).type().name())) break;
                    }
                    // 向后查找：下一个 RELATIONSHIP 的 i 位置记录此节点为 "prev"
                    for (int j = i + 1; j < segmentValues.size(); j++) {
                        if ("RELATIONSHIP".equals(segmentValues.get(j).type().name()) && prevNodeId[j] == null) {
                            prevNodeId[j] = gn.getId();
                        }
                        if ("NODE".equals(segmentValues.get(j).type().name())) break;
                    }
                }
            }

            // 第二遍：处理 RELATIONSHIP 段，使用预提取的相邻节点 ID
            for (int i = 0; i < segmentValues.size(); i++) {
                Value segVal = segmentValues.get(i);
                if ("RELATIONSHIP".equals(segVal.type().name())) {
                    try {
                        GraphEdge ge = buildEdgeFromValue(segVal, prevNodeId[i], nextNodeId[i]);
                        if (ge != null) {
                            String key = ge.getSourceId() + ":" + ge.getTargetId() + ":" + ge.getRelation().name();
                            edgeMap.putIfAbsent(key, ge);
                        }
                    } catch (Exception e) {
                        log.debug("路径边段解析失败（预期内，跳过）: {}", e.getMessage());
                    }
                }
            }
        }

        return KnowledgeGraphVO.builder()
                .nodes(new ArrayList<>(nodeMap.values()))
                .edges(new ArrayList<>(edgeMap.values()))
                .build();
    }

    /**
     * 从 Neo4j Value 构建 GraphNode.
     */
    private GraphNode buildNodeFromValue(Value nodeVal) {
        Map<String, Object> props = nodeVal.asMap();
        long neoId = nodeVal.asNode().id();
        String label = resolveLabel(nodeVal);
        GraphNodeType type = resolveType(nodeVal);

        // Book 节点含业务 id 属性 → 用业务 id；Keyword/Author/Subject 节点以 name 为唯一键
        // （见 KgSchemaInitializer），无 id 属性 → 退化为 Neo4j 内部 id()。可视化层据此匹配节点，
        // 前端不应使用非 Book 节点的 id 反查 MySQL（这些实体仅存于图数据库）。
        Long entityId = neoId;
        if (props.containsKey("id") && props.get("id") instanceof Number) {
            entityId = ((Number) props.get("id")).longValue();
        }

        GraphNode.GraphNodeBuilder builder = GraphNode.builder()
                .id(entityId)
                .label(label)
                .type(type);

        // 传递关键属性
        Map<String, Object> nodeProps = new HashMap<>();
        if (props.containsKey("pagerank")) nodeProps.put("pagerank", props.get("pagerank"));
        if (props.containsKey("borrowCount")) nodeProps.put("borrowCount", props.get("borrowCount"));
        if (!nodeProps.isEmpty()) builder.properties(nodeProps);

        return builder.build();
    }

    /**
     * 从 Neo4j Value 构建 GraphEdge.
     * <p>
     * Neo4j 5.x 的 elementId 为字符串格式（如 {@code "4:abc123def:0"}），不可被
     * {@link Long#parseLong(String)} 解析。此处使用 {@code buildGraphFromPaths}
     * 预提取的相邻节点业务 ID 作为 sourceId / targetId.
     *
     * @param relVal      关系 Value
     * @param sourceBizId 源节点业务 ID（从路径中前一个 NODE 提取），可为 null
     * @param targetBizId 目标节点业务 ID（从路径中后一个 NODE 提取），可为 null
     */
    private GraphEdge buildEdgeFromValue(Value relVal, Long sourceBizId, Long targetBizId) {
        Relationship rel = relVal.asRelationship();

        // 优先使用预提取的业务 ID，回退到 Neo4j 内部 id()
        long startId = sourceBizId != null ? sourceBizId : rel.startNodeId();
        long endId = targetBizId != null ? targetBizId : rel.endNodeId();

        String relType = rel.type();
        Map<String, Object> relProps = rel.asMap();

        double weight = 1.0;
        if (relProps.containsKey("weight") && relProps.get("weight") instanceof Number) {
            weight = ((Number) relProps.get("weight")).doubleValue();
        }

        GraphRelationType graphRelType = parseRelationType(relType);

        return GraphEdge.builder()
                .sourceId(startId)
                .targetId(endId)
                .relation(graphRelType)
                .weight(weight)
                .build();
    }

    /**
     * 解析节点的展示标签.
     */
    private String resolveLabel(Value nodeVal) {
        Map<String, Object> props = nodeVal.asMap();
        if (props.containsKey("title") && props.get("title") != null) {
            return props.get("title").toString();
        }
        if (props.containsKey("name") && props.get("name") != null) {
            return props.get("name").toString();
        }
        return "NODE";
    }

    /**
     * 解析节点类型.
     */
    private GraphNodeType resolveType(Value nodeVal) {
        var labels = nodeVal.asNode().labels();
        for (String lbl : labels) {
            try {
                return GraphNodeType.valueOf(lbl.toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }
        return GraphNodeType.KEYWORD;
    }

    /**
     * 解析关系类型枚举.
     */
    private GraphRelationType parseRelationType(String type) {
        try {
            return GraphRelationType.valueOf(type);
        } catch (IllegalArgumentException e) {
            return GraphRelationType.RELATED_TO;
        }
    }

    /**
     * 整数白名单校验：clamp depth 到 [min, max].
     */
    private int clamp(int value, int min, int max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }
}
