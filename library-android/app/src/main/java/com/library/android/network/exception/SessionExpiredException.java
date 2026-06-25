package com.library.android.network.exception;

/**
 * 会话失效异常 — HTTP 401，Refresh Token 也已无效.
 *
 * <p>UI 层应触发跳转至登录页 + 清栈。通常由 {@link com.library.android.network.SessionManager}
 * 全局广播驱动.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public class SessionExpiredException extends ApiException {
    public SessionExpiredException(String serverMessage) {
        super(401, serverMessage);
    }
}
