package com.library.android.network;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * 会话状态全局广播 — 用于在 OkHttp 拦截器/Authenticator 与 UI 层之间传递会话失效事件.
 *
 * <p>典型链路：
 * <ol>
 *   <li>{@link TokenAuthenticator} refresh 失败 → 调 {@link #notifyExpired()};</li>
 *   <li>{@link com.library.android.ui.main.MainActivity} 监听
 *       {@link #getSessionExpired()} → 跳转登录页 + 清栈.</li>
 * </ol>
 *
 * <p>使用 {@code MutableLiveData.postValue}, 线程安全；
 * 消费者侧调用 {@link #consume()} 显式清除（防止登录页旋转屏幕时再次触发）.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Singleton
public class SessionManager {

    private final MutableLiveData<Boolean> sessionExpired = new MutableLiveData<>(false);

    @Inject
    public SessionManager() {
    }

    public LiveData<Boolean> getSessionExpired() {
        return sessionExpired;
    }

    /** 在任意线程通知会话已失效. */
    public void notifyExpired() {
        sessionExpired.postValue(true);
    }

    /** UI 处理完会话失效后调用，重置状态防止重复跳转. */
    public void consume() {
        sessionExpired.postValue(false);
    }
}
