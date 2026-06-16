package com.library.security.ratelimit;

/**
 * 限流判定结果.
 *
 * @param allowed        是否放行
 * @param remaining      剩余可用令牌数（X-RateLimit-Remaining）
 * @param resetEpochSecond 重置时间（Unix 秒，X-RateLimit-Reset）
 * @param capacity       桶容量（X-RateLimit-Limit）
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public record RateLimitResult(boolean allowed, long remaining, long resetEpochSecond, int capacity) {
}
