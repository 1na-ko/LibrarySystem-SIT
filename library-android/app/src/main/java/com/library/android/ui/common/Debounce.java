package com.library.android.ui.common;

import android.os.SystemClock;
import android.view.View;

/**
 * 简单的点击防抖工具 — 在 {@link View#getTag(int)} 中记录最近一次点击时间戳.
 *
 * <p>典型用法：
 * <pre>{@code
 * btn.setOnClickListener(Debounce.wrap(v -> doSubmit()));
 * // 或显式判断:
 * btn.setOnClickListener(v -> {
 *     if (Debounce.allow(v)) doSubmit();
 * });
 * }</pre>
 *
 * <p>默认时间窗 {@value #DEFAULT_INTERVAL_MS} 毫秒，覆盖 ListAdapter 滑删/双击场景.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public final class Debounce {

    private static final int TAG_KEY = com.library.android.R.id.tag_debounce_timestamp;
    private static final long DEFAULT_INTERVAL_MS = 500L;

    private Debounce() {
    }

    /** 是否允许本次点击通过（未在防抖窗口内）. */
    public static boolean allow(View v) {
        return allow(v, DEFAULT_INTERVAL_MS);
    }

    public static boolean allow(View v, long intervalMs) {
        long now = SystemClock.elapsedRealtime();
        Object last = v.getTag(TAG_KEY);
        long lastTs = last instanceof Long ? (Long) last : 0L;
        if (now - lastTs < intervalMs) {
            return false;
        }
        v.setTag(TAG_KEY, now);
        return true;
    }

    /** 包装一个监听器，使其在防抖窗口内只触发一次. */
    public static View.OnClickListener wrap(View.OnClickListener delegate) {
        return wrap(delegate, DEFAULT_INTERVAL_MS);
    }

    public static View.OnClickListener wrap(View.OnClickListener delegate, long intervalMs) {
        return v -> {
            if (allow(v, intervalMs)) {
                delegate.onClick(v);
            }
        };
    }
}
