package com.library.android.network.exception;

/**
 * 网络异常 — IOException 等连接级失败的统一包装.
 *
 * <p>区别于 {@link ApiException}：网络异常代表请求未到达服务器或响应未读取完成.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public class NetworkException extends RuntimeException {
    public NetworkException(Throwable cause) {
        super("Network error: " + (cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName()), cause);
    }
}
