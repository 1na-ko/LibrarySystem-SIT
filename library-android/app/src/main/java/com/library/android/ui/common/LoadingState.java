package com.library.android.ui.common;

/**
 * UI 加载状态枚举 — 统一管理 Idle / Loading / Content / Empty / Error 五态.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public enum LoadingState {
    /** 空闲（初始态，未发起任何请求） */
    IDLE,
    /** 加载中 */
    LOADING,
    /** 内容展示 */
    CONTENT,
    /** 空数据 */
    EMPTY,
    /** 错误 */
    ERROR
}
