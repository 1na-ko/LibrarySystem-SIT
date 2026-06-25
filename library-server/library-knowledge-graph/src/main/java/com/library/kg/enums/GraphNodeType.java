package com.library.kg.enums;

import lombok.Getter;

/**
 * 知识图谱节点类型枚举.
 * <p>
 * 对应 OpenAPI {@code GraphNode.type} 的枚举值。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Getter
public enum GraphNodeType {

    /** 图书 */
    BOOK("图书"),
    /** 作者 */
    AUTHOR("作者"),
    /** 学科 */
    SUBJECT("学科"),
    /** 关键词 */
    KEYWORD("关键词"),
    /** 出版物 */
    PUBLICATION("出版物"),
    /** 会议 */
    CONFERENCE("会议");

    private final String description;

    GraphNodeType(String description) {
        this.description = description;
    }
}
