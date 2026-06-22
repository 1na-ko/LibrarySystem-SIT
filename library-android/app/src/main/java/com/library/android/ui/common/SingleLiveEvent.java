package com.library.android.ui.common;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 单次触发的 LiveData — 仅在 setValue/postValue 后第一次回调时通知 observer.
 *
 * <p>典型场景：网络错误 Snackbar、跳转事件、登录成功事件 — 这些事件不应在 Fragment 重建时
 * 重复触发（默认 LiveData 在 observe 时会立即收到最近一次值）.
 *
 * <p>实现来自 Google android-architecture 示例的经典模式.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public class SingleLiveEvent<T> extends MutableLiveData<T> {

    private static final String TAG = "SingleLiveEvent";

    private final AtomicBoolean pending = new AtomicBoolean(false);

    @MainThread
    @Override
    public void observe(@androidx.annotation.NonNull LifecycleOwner owner,
                        @androidx.annotation.NonNull final Observer<? super T> observer) {
        if (hasActiveObservers()) {
            android.util.Log.w(TAG, "Multiple observers registered but only one will be notified of changes.");
        }
        super.observe(owner, t -> {
            if (pending.compareAndSet(true, false)) {
                observer.onChanged(t);
            }
        });
    }

    @MainThread
    @Override
    public void setValue(@Nullable T t) {
        pending.set(true);
        super.setValue(t);
    }

    @Override
    public void postValue(@Nullable T value) {
        pending.set(true);
        super.postValue(value);
    }

    /** 用于明确"无参数"事件触发. */
    @MainThread
    public void call() {
        setValue(null);
    }
}
