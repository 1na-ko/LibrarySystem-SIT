package com.library.android.network.exception;

/**
 * 限流异常 — HTTP 429（Too Many Requests）.
 *
 * <p>触发后端 RateLimitFilter Lua 令牌桶限流（认证 100/min·用户，
 * 登录注册 20/min·IP）。UI 应提示"请求过于频繁，请稍后再试".
 *
 * <p>如有 Retry-After 响应头，{@link #getRetryAfterSeconds()} 返回建议等待时间.
 *
 * @since 1.0.0
 */
public class RateLimitException extends ApiException {

    private final int retryAfterSeconds;

    public RateLimitException(String serverMessage, int retryAfterSeconds) {
        super(429, serverMessage);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
