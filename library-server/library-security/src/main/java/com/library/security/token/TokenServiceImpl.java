package com.library.security.token;

import com.library.security.config.JwtProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * {@link TokenService} 基于 Redis 的实现.
 * <p>
 * Key 设计：{@code auth:refresh:{userId}} → jti，TTL 与 {@link JwtProperties#getRefreshTokenExpiration()}
 * 同步（默认 7d）。轮换通过 {@code refresh_rotate.lua} 原子执行，防止并发重放。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class TokenServiceImpl implements TokenService {

    /** Redis key 前缀 */
    private static final String KEY_PREFIX = "auth:refresh:";

    private final StringRedisTemplate redis;
    private final RedisScript<Long> rotateScript;
    private final Duration ttl;

    public TokenServiceImpl(StringRedisTemplate redis,
                            RedisScript<Long> rotateScript,
                            JwtProperties jwtProperties) {
        this.redis = redis;
        this.rotateScript = rotateScript;
        this.ttl = Duration.ofMillis(jwtProperties.getRefreshTokenExpiration());
    }

    @Override
    public void storeRefresh(long userId, String jti) {
        redis.opsForValue().set(KEY_PREFIX + userId, jti, ttl);
    }

    @Override
    public int rotate(long userId, String expectedJti, String newJti) {
        Long result = redis.execute(rotateScript,
                List.of(KEY_PREFIX + userId),
                expectedJti, newJti, String.valueOf(ttl.getSeconds()));
        return result == null ? 1 : Math.toIntExact(result);
    }

    @Override
    public void revoke(long userId) {
        if (!Boolean.TRUE.equals(redis.delete(KEY_PREFIX + userId))) {
            log.warn("Refresh Token 删除失败或 key 不存在: userId={}", userId);
        }
    }
}
