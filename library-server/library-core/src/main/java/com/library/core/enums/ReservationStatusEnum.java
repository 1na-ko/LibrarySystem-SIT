package com.library.core.enums;

import lombok.Getter;

/**
 * 预约状态枚举.
 * <p>
 * 对应 {@code reservation.status} 列（MySQL ENUM）。
 * 状态流转：{@code WAITING} → {@code NOTIFIED} → {@code RESERVED} → {@code COMPLETED}
 * ；或 {@code WAITING} → {@code EXPIRED} / {@code CANCELLED}。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Getter
public enum ReservationStatusEnum {

    /** 排队中 */
    WAITING("排队中"),
    /** 已通知 */
    NOTIFIED("已通知"),
    /** 已锁定（读者 48h 内确认） */
    RESERVED("已锁定"),
    /** 超时过期 */
    EXPIRED("已过期"),
    /** 借阅完成 */
    COMPLETED("已完成"),
    /** 已取消 */
    CANCELLED("已取消");

    /** 状态描述（仅展示用） */
    private final String description;

    ReservationStatusEnum(String description) {
        this.description = description;
    }
}
