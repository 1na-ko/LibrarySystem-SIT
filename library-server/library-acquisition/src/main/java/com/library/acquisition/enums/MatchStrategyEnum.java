package com.library.acquisition.enums;

import lombok.Getter;

/**
 * 查重匹配策略枚举（业务枚举，不落库）.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Getter
public enum MatchStrategyEnum {
    ISBN_EXACT("ISBN 精确匹配", 1.0),
    TITLE_FUZZY("标题模糊匹配", 0.85),
    AUTHOR_TITLE("作者+标题联合匹配", 0.7);

    private final String description;
    private final double weight;

    MatchStrategyEnum(String description, double weight) {
        this.description = description;
        this.weight = weight;
    }
}
