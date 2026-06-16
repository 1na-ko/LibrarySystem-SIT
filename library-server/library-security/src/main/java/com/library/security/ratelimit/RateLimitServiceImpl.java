package com.library.security.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * {@link RateLimitService} 基于 Redis Lua 令牌桶的实现.
 * <p>
 * Key 设计：{@code rl:{key}:tokens}（剩余令牌）与 {@code rl:{key}:ts}（上次补充时间戳），
 * TTL=120s（窗口过期回收）。Redis 不可用时降级放行，避免限流故障阻断主业务。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitServiceImpl implements RateLimitService {

    private static final String KEY_PREFIX = "rl:";
    private static final String TOKENS_SUFFIX = ":tokens";
    private static final String TS_SUFFIX = ":ts";
    /** key TTL（秒），令牌桶窗口回收 */
    private static final long TTL_SECONDS = 120L;

    private final StringRedisTemplate redis;
    private final RedisScript<List> tokenBucketScript;

    @Override
    public RateLimitResult tryAcquire(String key, int capacity, double refillPerSec) {
        List<?> result;
        try {
            result = redis.execute(tokenBucketScript,
                    List.of(KEY_PREFIX + key + TOKENS_SUFFIX, KEY_PREFIX + key + TS_SUFFIX),
                    String.valueOf(capacity),
                    String.valueOf(refillPerSec),
                    String.valueOf(System.currentTimeMillis()),
                    "1",
                    String.valueOf(TTL_SECONDS));
        } catch (Exception e) {
            // Redis 不可用时降级放行，避免限流故障阻断主业务
            log.warn("限流器 Redis 调用异常，降级放行: key={}, msg={}", key, e.getMessage());
            return new RateLimitResult(true, capacity, 0L, capacity);
        }

        if (result == null || result.size() < 3) {
            return new RateLimitResult(true, capacity, 0L, capacity);
        }
        long allowed = toLong(result.get(0));
        long remaining = toLong(result.get(1));
        long reset = toLong(result.get(2));
        return new RateLimitResult(allowed == 1L, remaining, reset, capacity);
    }

    /** 兼容 Lua 经 String 序列化器返回的 String 与 Long */
    private static long toLong(Object o) {
        if (o == null) {
            return 0L;
        }
        if (o instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(o.toString());
    }
}
