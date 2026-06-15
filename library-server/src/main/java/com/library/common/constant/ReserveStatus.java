package com.library.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReserveStatus {
    WAITING("等待中"),
    NOTIFIED("已通知"),
    FULFILLED("已满足"),
    CANCELLED("已取消"),
    EXPIRED("已过期");

    private final String description;
}
