package com.library.acquisition.enums;

import lombok.Getter;

/**
 * 谈判记录状态枚举（对齐 negotiation_record.status ENUM）.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Getter
public enum NegotiationStatusEnum {
    DRAFT("草稿"),
    IN_PROGRESS("进行中"),
    COMPLETED("已完成"),
    CANCELLED("已取消");

    private final String description;

    NegotiationStatusEnum(String description) { this.description = description; }
}
