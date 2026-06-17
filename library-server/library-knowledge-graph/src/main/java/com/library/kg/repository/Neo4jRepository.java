package com.library.kg.repository;

import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Neo4j 访问封装工具类.
 * <p>
 * 简单查询（无参 / 少量固定参数）通过 {@link Neo4jClient} 命令式链式 API；
 * 复杂参数化查询通过底层 {@link Driver} Session API 传递 Map 参数，
 * 兼顾 API 简洁性与参数安全性。
 * PageRank / 最短路径等图算法通过 {@link GdsAvailabilityProvider}
 * 实现运行时 GDS → 纯 Cypher / Java 降级。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class Neo4jRepository {

    private final Neo4jClient neo4jClient;
    private final Driver driver;
    private final GdsAvailabilityProvider gdsProvider;

    public Neo4jRepository(Neo4jClient neo4jClient, Driver driver, GdsAvailabilityProvider gdsProvider) {
        this.neo4jClient = neo4jClient;
        this.driver = driver;
        this.gdsProvider = gdsProvider;
    }

    // ---- 基础操作 ----

    /**
     * 执行无返回值 Cypher（写操作）.
     * <p>
     * 写入失败不静默吞噬：抛出 RuntimeException 由上层 Service 捕获并转译为业务异常，
     * 避免写入失败无感知。与只读 {@link #query} 返回空列表的容错语义明确区分。
     *
     * @throws RuntimeException 当 Cypher 执行失败时
     */
    public void execute(String cypher, Map<String, Object> params) {
        try (Session session = driver.session()) {
            session.run(cypher, params);
        } catch (Exception e) {
            log.error("Neo4j execute 异常: cypher={}, error={}", cypher, e.getMessage());
            throw new RuntimeException("Neo4j 写入失败: " + e.getMessage(), e);
        }
    }

    /**
     * 查询并映射为对象列表.
     */
    public <T> List<T> query(String cypher, Map<String, Object> params,
                             Function<Record, T> rowMapper) {
        try (Session session = driver.session()) {
            var result = session.run(cypher, params);
            return result.list(rowMapper);
        } catch (Exception e) {
            log.error("Neo4j 查询异常: cypher={}, error={}", cypher, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 查询返回单条记录.
     */
    public <T> T queryOne(String cypher, Map<String, Object> params,
                          Function<Record, T> rowMapper) {
        try (Session session = driver.session()) {
            var result = session.run(cypher, params);
            if (result.hasNext()) {
                return rowMapper.apply(result.next());
            }
            return null;
        } catch (Exception e) {
            log.error("Neo4j 查询异常: cypher={}, error={}", cypher, e.getMessage());
            return null;
        }
    }

    /**
     * 统计指定 label 的节点数量（白名单校验防 Cypher 注入）.
     */
    public long countNodes(String label) {
        // 白名单校验：仅允许已知的节点标签
        if (!ALLOWED_LABELS.contains(label)) {
            log.warn("countNodes 收到非法的 label 参数: {}, 已拒绝", label);
            return 0L;
        }
        try (Session session = driver.session()) {
            var result = session.run("MATCH (n:" + label + ") RETURN count(n) AS cnt");
            if (result.hasNext()) {
                return result.next().get("cnt").asLong();
            }
            return 0L;
        }
    }

    /** 允许的节点标签白名单（防 Cypher 注入） */
    private static final java.util.Set<String> ALLOWED_LABELS =
            java.util.Set.of("Book", "Author", "Keyword", "Subject", "Publication", "Conference");

    // ---- 节点与关系写入 ----

    /**
     * 幂等写入节点（MERGE 语义）.
     */
    public void saveNode(String label, Map<String, Object> matchProps, Map<String, Object> setProps) {
        Map<String, Object> params = new HashMap<>();
        StringBuilder cypher = new StringBuilder("MERGE (n:").append(label).append(" {");
        boolean first = true;
        for (String key : matchProps.keySet()) {
            if (!first) cypher.append(", ");
            first = false;
            cypher.append(key).append(": $m_").append(key);
            params.put("m_" + key, matchProps.get(key));
        }
        cypher.append("})");
        if (setProps != null && !setProps.isEmpty()) {
            cypher.append(" SET ");
            first = true;
            for (String key : setProps.keySet()) {
                if (!first) cypher.append(", ");
                first = false;
                cypher.append("n.").append(key).append(" = $s_").append(key);
                params.put("s_" + key, setProps.get(key));
            }
        }
        execute(cypher.toString(), params);
    }

    /**
     * 创建关系（MERGE 语义）.
     */
    public void saveRelationship(String fromLabel, Map<String, Object> fromMatch,
                                 String toLabel, Map<String, Object> toMatch,
                                 String relType, Map<String, Object> relProps) {
        Map<String, Object> params = new HashMap<>();
        StringBuilder cypher = new StringBuilder("MATCH (a:").append(fromLabel).append(" {");
        mergeEntryParams(fromMatch, "f", params, cypher);
        cypher.append("}) MATCH (b:").append(toLabel).append(" {");
        mergeEntryParams(toMatch, "t", params, cypher);
        cypher.append("}) MERGE (a)-[r:").append(relType).append("]->(b)");
        if (relProps != null && !relProps.isEmpty()) {
            cypher.append(" SET ");
            boolean relFirst = true;
            for (String key : relProps.keySet()) {
                if (!relFirst) cypher.append(", ");
                relFirst = false;
                cypher.append("r.").append(key).append(" = $r_").append(key);
                params.put("r_" + key, relProps.get(key));
            }
        }
        execute(cypher.toString(), params);
    }

    // ---- 批量写入（UNWIND 优化，消除 N+1 往返） ----

    /**
     * 批量 MERGE 节点（同 label，单一 key 属性匹配）.
     * <p>
     * 将 for 循环中逐条 {@code saveNode(label, {key: name}, {key: name})} 的 N+1 模式
     * 替换为单次 {@code UNWIND $rows AS row MERGE (n:<label> {<key>: row.name})}。
     *
     * @param label     节点标签
     * @param key       匹配属性名（如 "name"）
     * @param keyValues 属性值列表
     */
    public void batchMergeNodes(String label, String key, List<String> keyValues) {
        if (keyValues == null || keyValues.isEmpty()) return;

        Map<String, Object> params = Map.of("rows",
                keyValues.stream().map(v -> Map.of("name", (Object) v)).toList());
        String cypher = "UNWIND $rows AS row MERGE (n:" + label + " {" + key + ": row.name})";
        execute(cypher, params);
    }

    /**
     * 批量 MERGE 关系（从同一源节点到多个目标节点的同类型关系）.
     * <p>
     * 将 for 循环中逐条 {@code saveRelationship} 的 N+1 模式替换为单次
     * {@code UNWIND $rows AS row MATCH (src) MATCH (tgt) MERGE (src)-[r:<type>]->(tgt)}。
     *
     * @param srcLabel  源节点标签
     * @param srcKey    源节点匹配属性名
     * @param srcValue  源节点匹配属性值
     * @param tgtLabel  目标节点标签
     * @param tgtKey    目标节点匹配属性名（通常为 "name"）
     * @param relType   关系类型
     * @param tgtValues 目标节点属性值列表（含置信度）
     */
    public void batchMergeRelationships(String srcLabel, String srcKey, Object srcValue,
                                        String tgtLabel, String tgtKey, String relType,
                                        List<Map<String, Object>> tgtValues) {
        if (tgtValues == null || tgtValues.isEmpty()) return;

        Map<String, Object> params = new HashMap<>();
        params.put("srcVal", srcValue);
        params.put("rows", tgtValues);

        String cypher = "MATCH (src:" + srcLabel + " {" + srcKey + ": $srcVal}) "
                + "UNWIND $rows AS row "
                + "MATCH (tgt:" + tgtLabel + " {" + tgtKey + ": row.name}) "
                + "MERGE (src)-[r:" + relType + "]->(tgt) "
                + "SET r.confidence = coalesce(row.confidence, 0.5)";
        execute(cypher, params);
    }

    // ---- GDS 图算法 ----

    public Map<Long, Double> pageRank(String nodeLabel, String relType,
                                       double damping, int iterations) {
        if (gdsProvider.isAvailable()) {
            return pageRankViaGds(nodeLabel, relType);
        }
        log.info("GDS 不可用，使用 Java 侧 power iteration 降级");
        return pageRankViaPowerIteration(nodeLabel, relType, damping, iterations);
    }

    public List<Long> shortestPath(Long fromBookId, Long toBookId, String relType) {
        if (gdsProvider.isAvailable()) {
            return shortestPathViaGds(fromBookId, toBookId, relType);
        }
        log.info("GDS 不可用，使用 Cypher shortestPath() 降级");
        return shortestPathViaCypher(fromBookId, toBookId, relType);
    }

    // ---- 内部辅助 ----

    private void mergeEntryParams(Map<String, Object> props, String prefix,
                                   Map<String, Object> params, StringBuilder sb) {
        boolean first = true;
        for (Map.Entry<String, Object> entry : props.entrySet()) {
            if (!first) sb.append(", ");
            first = false;
            String key = prefix + "_" + entry.getKey();
            sb.append(entry.getKey()).append(": $").append(key);
            params.put(key, entry.getValue());
        }
    }

    // ---- GDS 实现 ----

    private Map<Long, Double> pageRankViaGds(String nodeLabel, String relType) {
        Map<Long, Double> scores = new HashMap<>();
        // 使用时间戳后缀避免并发调用时的图名冲突
        String graphName = "prGraph_" + System.currentTimeMillis() + "_" + Thread.currentThread().getId();
        try {
            execute("CALL gds.graph.project('" + graphName + "','" + nodeLabel
                    + "', {" + relType + ": {orientation: 'UNDIRECTED'}})", Map.of());
            List<Map.Entry<Long, Double>> results = query(
                    "CALL gds.pageRank.stream('" + graphName + "') YIELD nodeId, score "
                            + "RETURN nodeId AS entityId, score",
                    Map.of(),
                    rec -> Map.entry(rec.get("entityId").asLong(), rec.get("score").asDouble()));
            for (var entry : results) {
                scores.put(entry.getKey(), entry.getValue());
            }
            execute("CALL gds.graph.drop('" + graphName + "', false)", Map.of());
        } catch (Exception e) {
            log.warn("GDS PageRank 执行失败，降级 power iteration: {}", e.getMessage());
            // 确保清理 GDS 投影（可能因中途失败而残留）
            try { execute("CALL gds.graph.drop('" + graphName + "', false)", Map.of()); } catch (Exception ignored) {}
            scores.putAll(pageRankViaPowerIteration(nodeLabel, relType, 0.85, 20));
        }
        return scores;
    }

    private Map<Long, Double> pageRankViaPowerIteration(String nodeLabel, String relType,
                                                         double damping, int iterations) {
        String cypher = "MATCH (a:" + nodeLabel + ")-[:" + relType + "]-(b:" + nodeLabel + ") "
                + "RETURN id(a) AS src, id(b) AS tgt, count(*) AS w";
        List<Map<String, Object>> edges = query(cypher, Map.of(),
                rec -> Map.of("src", rec.get("src").asLong(),
                        "tgt", rec.get("tgt").asLong(),
                        "w", rec.get("w").asLong(1L)));
        if (edges.isEmpty()) return Collections.emptyMap();

        Map<Long, Integer> nodeIndex = new HashMap<>();
        for (var edge : edges) {
            Long src = (Long) edge.get("src");
            Long tgt = (Long) edge.get("tgt");
            nodeIndex.putIfAbsent(src, nodeIndex.size());
            nodeIndex.putIfAbsent(tgt, nodeIndex.size());
        }
        int n = nodeIndex.size();
        if (n == 0) return Collections.emptyMap();

        // 稀疏邻接表：List<Map<列, 权重>> — O(edges) 内存，避免稠密 n×n 矩阵
        List<Map<Integer, Double>> adj = new ArrayList<>(n);
        for (int i = 0; i < n; i++) adj.add(new java.util.HashMap<>());
        for (var edge : edges) {
            Long src = (Long) edge.get("src");
            Long tgt = (Long) edge.get("tgt");
            int si = nodeIndex.get(src);
            int ti = nodeIndex.get(tgt);
            adj.get(si).merge(ti, 1.0, Double::sum);
            adj.get(ti).merge(si, 1.0, Double::sum);
        }
        // 列归一化
        double[] colSum = new double[n];
        for (int i = 0; i < n; i++) {
            for (var entry : adj.get(i).entrySet()) {
                colSum[entry.getKey()] += entry.getValue();
            }
        }
        for (int i = 0; i < n; i++) {
            var it = adj.get(i).entrySet().iterator();
            while (it.hasNext()) {
                var entry = it.next();
                int j = entry.getKey();
                if (colSum[j] > 0) {
                    entry.setValue(entry.getValue() / colSum[j]);
                } else {
                    it.remove();
                }
            }
        }
        double[] rank = new double[n];
        double init = 1.0 / n;
        double teleport = (1.0 - damping) / n;
        for (int i = 0; i < n; i++) rank[i] = init;
        for (int iter = 0; iter < iterations; iter++) {
            double[] newRank = new double[n];
            for (int i = 0; i < n; i++) {
                double sum = 0;
                for (var entry : adj.get(i).entrySet()) {
                    sum += entry.getValue() * rank[entry.getKey()];
                }
                newRank[i] = damping * sum + teleport;
            }
            rank = newRank;
        }
        Map<Long, Double> scores = new HashMap<>();
        for (var entry : nodeIndex.entrySet()) {
            scores.put(entry.getKey(), rank[entry.getValue()]);
        }
        return scores;
    }

    private List<Long> shortestPathViaGds(Long from, Long to, String relType) {
        try {
            List<Object> result = query(
                    "MATCH (s:Book {id: $fromId}), (t:Book {id: $toId}) "
                            + "CALL gds.shortestPath.dijkstra.stream('citationGraph', {"
                            + "sourceNode: id(s), targetNode: id(t), relationshipWeightProperty: 'weight'}) "
                            + "YIELD nodeIds RETURN nodeIds LIMIT 1",
                    Map.of("fromId", from, "toId", to),
                    rec -> rec.get("nodeIds").asList());
            if (result.isEmpty()) return Collections.emptyList();
            return ((List<?>) result.get(0)).stream()
                    .map(id -> ((Number) id).longValue()).toList();
        } catch (Exception e) {
            log.warn("GDS Dijkstra 失败，降级: {}", e.getMessage());
            return shortestPathViaCypher(from, to, relType);
        }
    }

    private List<Long> shortestPathViaCypher(Long from, Long to, String relType) {
        int safeDepth = 5;
        String cypher = "MATCH p = shortestPath((s:Book {id: $fromId})-[:"
                + relType + "*1.." + safeDepth + "]-(t:Book {id: $toId})) "
                + "RETURN [n IN nodes(p) | n.id] AS nodeIds LIMIT 1";
        List<Object> result = query(cypher,
                Map.of("fromId", from, "toId", to),
                rec -> rec.get("nodeIds").asList());
        if (result.isEmpty()) return Collections.emptyList();
        return ((List<?>) result.get(0)).stream()
                .map(id -> ((Number) id).longValue()).toList();
    }
}
