package com.library.kg.vo;

import com.library.kg.model.GraphEdge;
import com.library.kg.model.GraphNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 知识图谱可视化 VO.
 * <p>
 * 对应 OpenAPI {@code KnowledgeGraphVO} Schema，
 * 包含某图书为中心的节点与边集合。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeGraphVO {

    /** 节点集合 */
    private List<GraphNode> nodes;

    /** 边集合 */
    private List<GraphEdge> edges;
}
