package com.library.acquisition.enums;

import lombok.Getter;

/**
 * 资源类别枚举（对齐 deal_record.category / electronic_resource.category ENUM）.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Getter
public enum ResourceCategoryEnum {
    JOURNAL("期刊"),
    DATABASE("数据库"),
    EBOOK("电子书"),
    CONFERENCE("会议论文");

    private final String description;

    ResourceCategoryEnum(String description) { this.description = description; }
}
