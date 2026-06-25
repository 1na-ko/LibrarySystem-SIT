package com.library.kg.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 文献溯源路径模型.
 * <p>
 * 对应 OpenAPI {@code TraceGraph.paths[]} 元素，
 * 描述一条多跳引用路径的节点与边。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TracePath {

    /** 路径上的节点集合 */
    private List<GraphNode> nodes;

    /** 路径上的边集合 */
    private List<GraphEdge> edges;

    /** 路径深度（跳数） */
    private int depth;

    /** 路径总权重（Dijkstra totalCost） */
    private Double totalWeight;
}
