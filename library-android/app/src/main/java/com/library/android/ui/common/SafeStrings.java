package com.library.android.ui.common;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * 字符串安全工具 — 集中处理 Android 端常见的 null/越界/裁剪场景，避免散落的 substring NPE.
 *
 * <p>历史问题：多个 Fragment 直接 {@code item.getXxx().substring(0, 7)}，
 * 当后端因数据缺失返回 null 或字段长度异常短时即崩溃。集中到此处统一兜底.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public final class SafeStrings {

    private SafeStrings() {
    }

    /** null/empty → 空串；否则返回 trim 后的原值. */
    @NonNull
    public static String defaultIfEmpty(@Nullable String s) {
        return s == null ? "" : s.trim();
    }

    /** null/empty → fallback；否则返回 trim 后的原值. */
    @NonNull
    public static String defaultIfEmpty(@Nullable String s, @NonNull String fallback) {
        if (s == null || s.trim().isEmpty()) return fallback;
        return s.trim();
    }

    /**
     * 安全 substring — 自动 clamp 起止索引，避免 IndexOutOfBoundsException.
     *
     * @return 截取结果（null 输入返回空串）
     */
    @NonNull
    public static String safeSubstring(@Nullable String s, int start, int end) {
        if (s == null) return "";
        int len = s.length();
        int safeStart = Math.max(0, Math.min(start, len));
        int safeEnd = Math.max(safeStart, Math.min(end, len));
        return s.substring(safeStart, safeEnd);
    }

    /** 取日期字符串前 10 位（YYYY-MM-DD），null 安全. */
    @NonNull
    public static String safeDate(@Nullable String dateStr) {
        return safeSubstring(dateStr, 0, 10);
    }

    /** 取年月（YYYY-MM）前 7 位，null 安全. */
    @NonNull
    public static String safeMonth(@Nullable String dateStr) {
        return safeSubstring(dateStr, 0, 7);
    }
}
