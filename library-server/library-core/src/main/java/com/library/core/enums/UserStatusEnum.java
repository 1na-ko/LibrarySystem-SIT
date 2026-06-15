package com.library.core.enums;

import lombok.Getter;

/**
 * 用户账户状态枚举.
 * <p>
 * 对应 {@code sys_user.status} 列（MySQL ENUM）。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Getter
public enum UserStatusEnum {

    /** 正常 */
    ACTIVE("正常"),
    /** 冻结（可解冻） */
    FROZEN("冻结"),
    /** 禁用（不可恢复） */
    DISABLED("禁用");

    /** 状态描述（仅展示用，不落库） */
    private final String description;

    UserStatusEnum(String description) {
        this.description = description;
    }
}
