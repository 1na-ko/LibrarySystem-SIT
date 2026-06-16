package com.library.acquisition.enums;

import lombok.Getter;

/**
 * 电子资源状态枚举（对齐 electronic_resource.status ENUM）.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Getter
public enum ResourceStatusEnum {
    ACTIVE("在用"),
    TRIAL("试用"),
    EXPIRED("已过期"),
    PENDING("待采购");

    private final String description;

    ResourceStatusEnum(String description) { this.description = description; }
}
