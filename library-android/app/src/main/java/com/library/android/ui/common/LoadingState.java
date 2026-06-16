package com.library.android.ui.common;

/**
 * UI 加载状态枚举 — 统一管理 Loading / Content / Empty / Error 四态.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public enum LoadingState {
    /** 加载中 */
    LOADING,
    /** 内容展示 */
    CONTENT,
    /** 空数据 */
    EMPTY,
    /** 错误 */
    ERROR
}
