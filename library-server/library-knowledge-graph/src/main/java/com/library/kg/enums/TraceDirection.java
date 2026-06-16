package com.library.kg.enums;

import lombok.Getter;

/**
 * 文献溯源方向枚举.
 * <p>
 * FORWARD = 前向溯源（谁引用了本文？向后引用链）、
 * BACKWARD = 后向溯源（本文引用了谁？向前引用链）、
 * BOTH = 双向溯源。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Getter
public enum TraceDirection {

    FORWARD("前向溯源"),
    BACKWARD("后向溯源"),
    BOTH("双向溯源");

    private final String description;

    TraceDirection(String description) {
        this.description = description;
    }
}
