package com.library.security.token;

import com.library.security.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TokenServiceImpl 单元测试.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TokenServiceImpl Refresh Token 存储")
class TokenServiceImplTest {

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private RedisScript<Long> rotateScript;

    @Mock
    private JwtProperties jwtProperties;

    private TokenServiceImpl tokenService;

    @BeforeEach
    void setUp() {
        // 默认值：Refresh Token 有效期 7 天（604,800,000ms）
        when(jwtProperties.getRefreshTokenExpiration()).thenReturn(604_800_000L);
        tokenService = new TokenServiceImpl(redis, rotateScript, jwtProperties);
    }

    @Test
    @DisplayName("storeRefresh 应以 7d TTL 写入 auth:refresh:{userId}")
    void shouldStoreRefreshWithTtl() {
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);

        tokenService.storeRefresh(123L, "jti-abc");

        verify(valueOps).set(eq("auth:refresh:123"), eq("jti-abc"), any(Duration.class));
    }

    @Test
    @DisplayName("rotate 脚本返回 0 时应判定为正常轮换")
    void shouldReturnZeroWhenRotateSucceeds() {
        when(redis.execute(eq(rotateScript), anyList(), eq("old"), eq("new"), any()))
                .thenReturn(0L);

        int result = tokenService.rotate(1L, "old", "new");

        assertThat(result).isZero();
    }

    @Test
    @DisplayName("rotate 脚本返回 1 时应判定为重放")
    void shouldReturnOneWhenReplayDetected() {
        when(redis.execute(eq(rotateScript), anyList(), eq("old"), eq("new"), any()))
                .thenReturn(1L);

        int result = tokenService.rotate(1L, "old", "new");

        assertThat(result).isEqualTo(1);
    }

    @Test
    @DisplayName("revoke 应删除 auth:refresh:{userId} 并写 auth:logout:{userId} 时间戳")
    void shouldDeleteKeyOnRevoke() {
        org.springframework.data.redis.core.ValueOperations<String, String> valueOps =
                org.mockito.Mockito.mock(org.springframework.data.redis.core.ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);

        tokenService.revoke(42L);

        verify(redis).delete("auth:refresh:42");
        // logout 时间戳：key=auth:logout:{userId}，value 为当前 epoch 秒，TTL=AT 有效期
        verify(valueOps).set(eq("auth:logout:42"), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(java.time.Duration.class));
    }
}
