package com.library.acquisition.enums;

import lombok.Getter;

/**
 * 采购缺口优先级枚举（业务枚举，不落库）.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Getter
public enum GapPriorityEnum {
    CRITICAL("紧急"),
    HIGH("高"),
    MEDIUM("中"),
    LOW("低");

    private final String description;

    GapPriorityEnum(String description) { this.description = description; }
}
