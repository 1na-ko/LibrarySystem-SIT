package com.library.core.enums;

import lombok.Getter;

/**
 * 借阅状态枚举.
 * <p>
 * 对应 {@code borrow_record.status} 列（MySQL ENUM）。
 * 状态流转：{@code BORROWED} → {@code RENEWED} → {@code RETURNED} / {@code OVERDUE}。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Getter
public enum BorrowStatusEnum {

    /** 借出（初始状态） */
    BORROWED("借出"),
    /** 已续借 */
    RENEWED("已续借"),
    /** 已归还 */
    RETURNED("已归还"),
    /** 超期未还 */
    OVERDUE("超期");

    /** 状态描述（仅展示用） */
    private final String description;

    BorrowStatusEnum(String description) {
        this.description = description;
    }
}
