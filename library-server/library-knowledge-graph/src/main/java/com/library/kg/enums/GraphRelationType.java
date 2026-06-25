package com.library.kg.enums;

import lombok.Getter;

/**
 * 知识图谱关系类型枚举.
 * <p>
 * 对应 OpenAPI {@code GraphEdge.relation} 的枚举值和架构文档 §7.3 本体模型。
 * 状态流转：关系一旦创建即持久化，无状态流转。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Getter
public enum GraphRelationType {

    /** Book → Author */
    AUTHORED_BY("撰写"),
    /** Book → Subject */
    BELONGS_TO("属于"),
    /** Book → Book（引用） */
    CITES("引用"),
    /** Book → Book（同被引） */
    CO_CITED("同被引"),
    /** Book → Keyword */
    HAS_KEYWORD("包含关键词"),
    /** Subject ↔ Subject / Keyword ↔ Keyword（主题关联） */
    RELATED_TO("关联"),
    /** Book → Publication */
    PUBLISHED_IN("发表于"),
    /** Book → Conference */
    PRESENTED_AT("发表于会议");

    private final String description;

    GraphRelationType(String description) {
        this.description = description;
    }
}
