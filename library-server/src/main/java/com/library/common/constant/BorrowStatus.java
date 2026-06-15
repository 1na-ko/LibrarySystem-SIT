package com.library.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BorrowStatus {
    BORROWED("借阅中"),
    RETURNED("已归还"),
    OVERDUE("已超期"),
    RENEWED("已续借");

    private final String description;
}
