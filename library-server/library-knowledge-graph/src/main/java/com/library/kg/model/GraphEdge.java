package com.library.kg.model;

import com.library.kg.enums.GraphRelationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识图谱关系模型.
 * <p>
 * 对应 OpenAPI {@code GraphEdge} Schema。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphEdge {

    /** 源节点 ID */
    private Long sourceId;

    /** 目标节点 ID */
    private Long targetId;

    /** 关系类型 */
    private GraphRelationType relation;

    /** 关系权重（控制展示粗细） */
    private Double weight;
}
