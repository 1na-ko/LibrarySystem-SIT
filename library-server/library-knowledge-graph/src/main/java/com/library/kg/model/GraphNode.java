package com.library.kg.model;

import com.library.kg.enums.GraphNodeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 知识图谱节点模型.
 * <p>
 * 对应 OpenAPI {@code GraphNode} Schema。
 * 用于图谱可视化数据传输。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphNode {

    /** 节点 ID（Neo4j 内部 id 或业务属性 id） */
    private Long id;

    /** 显示标签 */
    private String label;

    /** 节点类型 */
    private GraphNodeType type;

    /** 附加属性（如 pagerank、cluster 等） */
    private Map<String, Object> properties;
}
