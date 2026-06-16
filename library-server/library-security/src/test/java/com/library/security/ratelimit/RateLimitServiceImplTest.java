package com.library.security.ratelimit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * RateLimitServiceImpl 单元测试.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitServiceImpl 令牌桶限流")
class RateLimitServiceImplTest {

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private RedisScript<List> tokenBucketScript;

    @InjectMocks
    private RateLimitServiceImpl rateLimitService;

    @Test
    @DisplayName("脚本返回 allowed=1 时应放行并带剩余令牌")
    void shouldAllowWhenTokensRemain() {
        when(redis.execute(eq(tokenBucketScript), anyList(), any(Object[].class)))
                .thenReturn(List.of(1L, 99L, 1000L));

        RateLimitResult result = rateLimitService.tryAcquire("login:1.2.3.4", 100, 1.66);

        assertThat(result.allowed()).isTrue();
        assertThat(result.remaining()).isEqualTo(99L);
        assertThat(result.resetEpochSecond()).isEqualTo(1000L);
        assertThat(result.capacity()).isEqualTo(100);
    }

    @Test
    @DisplayName("脚本返回 allowed=0 时应拒绝")
    void shouldDenyWhenNoTokens() {
        when(redis.execute(eq(tokenBucketScript), anyList(), any(Object[].class)))
                .thenReturn(List.of(0L, 0L, 2000L));

        RateLimitResult result = rateLimitService.tryAcquire("login:1.2.3.4", 20, 0.33);

        assertThat(result.allowed()).isFalse();
        assertThat(result.remaining()).isZero();
    }

    @Test
    @DisplayName("Lua 返回 String 类型数字时应正确解析")
    void shouldParseStringResults() {
        // StringRedisTemplate 序列化器可能将 Lua 数字返回为 String
        when(redis.execute(eq(tokenBucketScript), anyList(), any(Object[].class)))
                .thenReturn(List.of("1", "5", "1500"));

        RateLimitResult result = rateLimitService.tryAcquire("k", 100, 1.0);

        assertThat(result.allowed()).isTrue();
        assertThat(result.remaining()).isEqualTo(5L);
        assertThat(result.resetEpochSecond()).isEqualTo(1500L);
    }

    @Test
    @DisplayName("Redis 异常时应降级放行（不阻断业务）")
    void shouldDegradeWhenRedisThrows() {
        when(redis.execute(eq(tokenBucketScript), anyList(), any(Object[].class)))
                .thenThrow(new RuntimeException("Redis 连接失败"));

        RateLimitResult result = rateLimitService.tryAcquire("k", 100, 1.0);

        assertThat(result.allowed()).isTrue();
        assertThat(result.capacity()).isEqualTo(100);
    }

    @Test
    @DisplayName("脚本返回 null 时应降级放行")
    void shouldDegradeWhenResultNull() {
        when(redis.execute(eq(tokenBucketScript), anyList(), any(Object[].class)))
                .thenReturn(null);

        RateLimitResult result = rateLimitService.tryAcquire("k", 100, 1.0);

        assertThat(result.allowed()).isTrue();
    }
}
