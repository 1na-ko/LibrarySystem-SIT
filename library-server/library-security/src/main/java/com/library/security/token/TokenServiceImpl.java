package com.library.security.token;

import com.library.security.config.JwtProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * {@link TokenService} 基于 Redis 的实现.
 * <p>
 * Key 设计：
 * <ul>
 *   <li>{@code auth:refresh:{userId}} → jti，TTL 与 {@link JwtProperties#getRefreshTokenExpiration()} 同步（默认 7d）；
 *       轮换通过 {@code refresh_rotate.lua} 原子执行，防止并发重放</li>
 *   <li>{@code auth:logout:{userId}} → epoch 秒时间戳，TTL 与 {@link JwtProperties#getAccessTokenExpiration()} 同步（默认 2h）；
 *       AT 的 iat ≤ 此值即视为已登出（无状态 AT 设计下的撤销机制）</li>
 * </ul>
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class TokenServiceImpl implements TokenService {

    /** Redis key 前缀：当前有效 Refresh Token jti */
    private static final String KEY_PREFIX = "auth:refresh:";
    /** Redis key 前缀：用户最近登出的 epoch 秒时间戳，用于撤销旧 AT */
    private static final String LOGOUT_KEY_PREFIX = "auth:logout:";

    private final StringRedisTemplate redis;
    private final RedisScript<Long> rotateScript;
    private final Duration refreshTtl;
    private final Duration accessTtl;

    public TokenServiceImpl(StringRedisTemplate redis,
                            RedisScript<Long> rotateScript,
                            JwtProperties jwtProperties) {
        this.redis = redis;
        this.rotateScript = rotateScript;
        this.refreshTtl = Duration.ofMillis(jwtProperties.getRefreshTokenExpiration());
        // 登出时间戳 TTL 与 AT 有效期同步——AT 自然过期后无需保留登出标记
        this.accessTtl = Duration.ofMillis(jwtProperties.getAccessTokenExpiration());
    }

    @Override
    public void storeRefresh(long userId, String jti) {
        redis.opsForValue().set(KEY_PREFIX + userId, jti, refreshTtl);
    }

    @Override
    public int rotate(long userId, String expectedJti, String newJti) {
        Long result = redis.execute(rotateScript,
                List.of(KEY_PREFIX + userId),
                expectedJti, newJti, String.valueOf(refreshTtl.getSeconds()));
        return result == null ? 1 : Math.toIntExact(result);
    }

    @Override
    public void revoke(long userId) {
        // 删除 RT 记录
        if (!Boolean.TRUE.equals(redis.delete(KEY_PREFIX + userId))) {
            log.warn("Refresh Token 删除失败或 key 不存在: userId={}", userId);
        }
        // 记录登出时间戳，使该用户在 AT TTL 内签发的所有 AT 立即失效
        long nowEpochSeconds = Instant.now().getEpochSecond();
        redis.opsForValue().set(LOGOUT_KEY_PREFIX + userId,
                String.valueOf(nowEpochSeconds), accessTtl);
    }

    @Override
    public boolean isAccessTokenLoggedOut(long userId, long iatEpochSeconds) {
        String ts = redis.opsForValue().get(LOGOUT_KEY_PREFIX + userId);
        if (ts == null) {
            return false;
        }
        try {
            // iat ≤ logoutTs 即视为已登出（同秒边界保守判定为已失效，防止 1 秒内 logout+复用）
            return iatEpochSeconds <= Long.parseLong(ts);
        } catch (NumberFormatException e) {
            log.warn("登出时间戳解析失败: userId={}, raw={}", userId, ts);
            return false;
        }
    }
}
