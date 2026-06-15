package com.library.security.token;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * {@link TokenService} 基于 Redis 的实现.
 * <p>
 * Key 设计：{@code auth:refresh:{userId}} → jti，TTL=7d。
 * 轮换通过 {@code refresh_rotate.lua} 原子执行，防止并发重放。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    /** Redis key 前缀 */
    private static final String KEY_PREFIX = "auth:refresh:";
    /** Refresh Token 存活时长（7 天） */
    private static final Duration TTL = Duration.ofDays(7);

    private final StringRedisTemplate redis;
    private final RedisScript<Long> rotateScript;

    @Override
    public void storeRefresh(long userId, String jti) {
        redis.opsForValue().set(KEY_PREFIX + userId, jti, TTL);
    }

    @Override
    public int rotate(long userId, String expectedJti, String newJti) {
        Long result = redis.execute(rotateScript,
                List.of(KEY_PREFIX + userId),
                expectedJti, newJti, String.valueOf(TTL.getSeconds()));
        return result == null ? 1 : Math.toIntExact(result);
    }

    @Override
    public void revoke(long userId) {
        redis.delete(KEY_PREFIX + userId);
    }
}
