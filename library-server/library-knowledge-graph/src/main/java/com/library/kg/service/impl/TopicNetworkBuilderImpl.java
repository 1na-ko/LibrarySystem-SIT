package com.library.kg.service.impl;

import com.library.kg.config.KnowledgeGraphProperties;
import com.library.kg.enums.GraphNodeType;
import com.library.kg.enums.GraphRelationType;
import com.library.kg.model.GraphEdge;
import com.library.kg.model.GraphNode;
import com.library.kg.repository.Neo4jRepository;
import com.library.kg.service.TopicNetworkBuilder;
import com.library.kg.vo.KnowledgeGraphVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Record;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 主题关联网络构建服务实现.
 * <p>
 * 算法：关键词共现矩阵 → Jaccard 相似度 → {@code RELATED_TO} 边 →
 * PageRank 中心度计算（GDS 优先，不可用时 Java 侧 power iteration 降级）.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TopicNetworkBuilderImpl implements TopicNetworkBuilder {

    private final Neo4jRepository neo4jRepository;
    private final KnowledgeGraphProperties kgProperties;

    private static final String NODE_KEYWORD = "Keyword";
    private static final String REL_RELATED_TO = "RELATED_TO";

    @Override
    public void buildTopicNetwork() {
        log.info("开始构建主题关联网络...");

        // Step 1: 关键词共现 + Jaccard → RELATED_TO 边
        buildRelatedToEdges();

        // Step 2: PageRank 计算中心度
        Map<Long, Double> pagerank = neo4jRepository.pageRank(
                NODE_KEYWORD, REL_RELATED_TO,
                kgProperties.getPagerankDamping(),
                kgProperties.getPagerankIterations());

        if (pagerank.isEmpty()) {
            log.info("主题网络构建完成：无关键词共现关系");
            return;
        }

        // Step 3: 将 PageRank 分数写回 Keyword 节点
        // 注意：Keyword 节点以 name 为唯一键（见 KgSchemaInitializer），不含业务 id 属性，
        // 故 PageRank 返回的节点标识为 Neo4j 内部 id()，此处用 id(k) 精确定位节点写回。
        // 使用 UNWIND 批量写入，消除 N+1 Cypher 往返。
        if (!pagerank.isEmpty()) {
            List<Map<String, Object>> rows = pagerank.entrySet().stream()
                    .map(e -> Map.<String, Object>of("kid", e.getKey(), "score", e.getValue()))
                    .toList();
            String cypher = """
                    UNWIND $rows AS row
                    MATCH (k:Keyword) WHERE id(k) = row.kid
                    SET k.pagerank = row.score""";
            neo4jRepository.execute(cypher, Map.of("rows", rows));
        }

        log.info("主题关联网络构建完成：{} 个关键词节点已计算 PageRank", pagerank.size());
    }

    @Override
    public KnowledgeGraphVO buildSubjectNetwork(String subjectName, int topK) {
        // 查询该 Subject 下的 Top-K 关键词（按 PageRank 降序）
        // 关键：MATCH 路径 (s)<-[:BELONGS_TO]-(b:Book)-[:HAS_KEYWORD]->(k) 在多本图书共享同一关键词时
        // 会产生 N 条路径，必须用 DISTINCT 去重，否则前端会看到同一关键词重复出现 N 次。
        String cypher = """
                MATCH (s:Subject {name: $subjectName})<-[:BELONGS_TO]-(:Book)-[:HAS_KEYWORD]->(k:Keyword)
                WHERE k.pagerank IS NOT NULL
                RETURN DISTINCT id(k) AS id, k.name AS label, k.pagerank AS pagerank
                ORDER BY k.pagerank DESC LIMIT $topK
                """;
        List<GraphNode> nodes = neo4jRepository.query(cypher,
                Map.of("subjectName", subjectName, "topK", topK),
                (rec) -> GraphNode.builder()
                        .id(rec.get("id").asLong())
                        .label(rec.get("label").asString())
                        .type(GraphNodeType.KEYWORD)
                        .properties(Map.of("pagerank", rec.get("pagerank").asDouble()))
                        .build());

        if (nodes.isEmpty()) {
            return KnowledgeGraphVO.builder().nodes(List.of()).edges(List.of()).build();
        }

        // 查询这些关键词之间的 RELATED_TO 边（参数化查询，防 Cypher 注入）
        List<Long> ids = nodes.stream().map(GraphNode::getId).toList();
        List<GraphEdge> edges = new ArrayList<>();
        if (ids.size() >= 2) {
            // 无向匹配 RELATED_TO：无论关系创建方向如何都能命中（id(k1)<id(k2) 保证不重复）
            String edgeCypher = "MATCH (k1:Keyword)-[r:RELATED_TO]-(k2:Keyword) "
                    + "WHERE id(k1) IN $ids AND id(k2) IN $ids "
                    + "AND id(k1) < id(k2) "
                    + "RETURN id(k1) AS sourceId, id(k2) AS targetId, r.weight AS weight";
            edges = neo4jRepository.query(edgeCypher, Map.of("ids", ids),
                    (rec) -> GraphEdge.builder()
                            .sourceId(rec.get("sourceId").asLong())
                            .targetId(rec.get("targetId").asLong())
                            .relation(GraphRelationType.RELATED_TO)
                            .weight(rec.get("weight").asDouble())
                            .build());
        }

        return KnowledgeGraphVO.builder()
                .nodes(nodes)
                .edges(edges)
                .build();
    }

    // ---- 内部 ----

    /**
     * 计算关键词共现的 Jaccard 相似度，并创建 RELATED_TO 边.
     * <p>
     * Cypher 参照架构文档 §7.4 算法骨架.
     */
    private void buildRelatedToEdges() {
        double threshold = kgProperties.getTopicJaccardThreshold();
        // Jaccard = coOccurrence / (freq1 + freq2 - coOccurrence)
        // freq1 = COUNT(DISTINCT b1) WHERE b1 HAS_KEYWORD k1（该关键词关联的图书数）
        // freq2 = COUNT(DISTINCT b2) WHERE b2 HAS_KEYWORD k2（该关键词关联的图书数）
        String cypher = """
                MATCH (k1:Keyword)<-[:HAS_KEYWORD]-(b:Book)-[:HAS_KEYWORD]->(k2:Keyword)
                WHERE id(k1) < id(k2)
                WITH k1, k2, COUNT(DISTINCT b) AS coOccurrence
                OPTIONAL MATCH (k1)<-[:HAS_KEYWORD]-(b1:Book)
                WITH k1, k2, coOccurrence, COUNT(DISTINCT b1) AS freq1
                OPTIONAL MATCH (k2)<-[:HAS_KEYWORD]-(b2:Book)
                WITH k1, k2, coOccurrence, freq1, COUNT(DISTINCT b2) AS freq2
                WITH k1, k2, coOccurrence * 1.0 / (freq1 + freq2 - coOccurrence) AS jaccard
                WHERE jaccard > $threshold AND jaccard <= 1.0
                MERGE (k1)-[r:RELATED_TO]->(k2)
                SET r.weight = jaccard
                RETURN count(r) AS createdEdges
                """;
        List<Long> result = neo4jRepository.query(cypher,
                Map.of("threshold", threshold),
                (rec) -> rec.get("createdEdges").asLong());
        long edgeCount = result.isEmpty() ? 0 : result.get(0);
        log.info("共现分析完成：创建/更新 {} 条 RELATED_TO 边 (Jaccard 阈值={})", edgeCount, threshold);
    }
}
