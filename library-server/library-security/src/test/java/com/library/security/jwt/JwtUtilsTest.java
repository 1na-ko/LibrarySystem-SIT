package com.library.security.jwt;

import com.library.security.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JwtUtils 单元测试.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@DisplayName("JwtUtils JWT 工具")
class JwtUtilsTest {

    /** 64 字节 ASCII 密钥，满足 HS256 ≥32 字节要求 */
    private static final String SECRET = "0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF";

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret(SECRET);
        props.setAccessTokenExpiration(1000L);
        props.setRefreshTokenExpiration(604_800_000L);
        jwtUtils = new JwtUtils(props);
    }

    @Test
    @DisplayName("生成 access token 应可解析且 claims 正确")
    void shouldGenerateAndParseAccessToken() {
        String token = jwtUtils.generateAccessToken(123L, "alice", "ADMIN");

        Claims claims = jwtUtils.parse(token);
        assertThat(claims.getSubject()).isEqualTo("123");
        assertThat(claims.get(JwtUtils.CLAIM_USERNAME)).isEqualTo("alice");
        assertThat(claims.get(JwtUtils.CLAIM_ROLE)).isEqualTo("ADMIN");
        assertThat(jwtUtils.isAccess(claims)).isTrue();
        assertThat(jwtUtils.isRefresh(claims)).isFalse();
        assertThat(claims.getId()).isNotBlank();
    }

    @Test
    @DisplayName("生成 refresh token 应含 jti 且 type=refresh")
    void shouldGenerateRefreshTokenWithJti() {
        JwtUtils.RefreshTokenData rt = jwtUtils.generateRefreshToken(123L);

        assertThat(rt.jti()).isNotBlank();
        assertThat(rt.token()).isNotBlank();

        Claims claims = jwtUtils.parse(rt.token());
        assertThat(claims.getSubject()).isEqualTo("123");
        assertThat(jwtUtils.isRefresh(claims)).isTrue();
        assertThat(jwtUtils.isAccess(claims)).isFalse();
        assertThat(claims.getId()).isEqualTo(rt.jti());
    }

    @Test
    @DisplayName("每次生成 refresh token 的 jti 应不同")
    void shouldGenerateDifferentJtiEachTime() {
        JwtUtils.RefreshTokenData r1 = jwtUtils.generateRefreshToken(1L);
        JwtUtils.RefreshTokenData r2 = jwtUtils.generateRefreshToken(1L);
        assertThat(r1.jti()).isNotEqualTo(r2.jti());
    }

    @Test
    @DisplayName("篡改签名的 token 应抛 JwtException")
    void shouldThrowWhenSignatureTampered() {
        String token = jwtUtils.generateAccessToken(1L, "a", "STUDENT");
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        assertThatThrownBy(() -> jwtUtils.parse(tampered))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("过期的 token 应抛 ExpiredJwtException")
    void shouldThrowWhenExpired() throws InterruptedException {
        // accessTokenExpiration=1000ms
        String token = jwtUtils.generateAccessToken(1L, "a", "STUDENT");
        Thread.sleep(1100L);

        assertThatThrownBy(() -> jwtUtils.parse(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("用不同密钥签发的 token 在新密钥下应解析失败")
    void shouldFailWhenDifferentSecret() {
        String token = jwtUtils.generateAccessToken(1L, "a", "STUDENT");

        JwtProperties other = new JwtProperties();
        other.setSecret("ABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZ");
        JwtUtils otherJwt = new JwtUtils(other);

        assertThatThrownBy(() -> otherJwt.parse(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("密钥不足 32 字节时构造应抛异常（fail-fast）")
    void shouldThrowWhenSecretTooShort() {
        JwtProperties weak = new JwtProperties();
        weak.setSecret("short");

        assertThatThrownBy(() -> new JwtUtils(weak))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("getAccessTokenExpiresInSeconds 应返回秒数")
    void shouldReturnExpiresInSeconds() {
        assertThat(jwtUtils.getAccessTokenExpiresInSeconds()).isEqualTo(1L);
    }
}
