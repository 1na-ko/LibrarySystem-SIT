package com.library.security.jwt;

import com.library.security.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 工具类（基于 jjwt 0.12.x API）.
 * <p>
 * 签发两类令牌：
 * <ul>
 *   <li><b>Access Token</b>：携带 userId/username/role/type=access，2h 有效，无状态不落库</li>
 *   <li><b>Refresh Token</b>：携带 userId/type=refresh + jti，7d 有效，jti 存 Redis 校验（见 {@code TokenService}）</li>
 * </ul>
 * 签名算法 HMAC-SHA256。{@link #parse(String)} 验签失败抛 {@link JwtException}，
 * 过期抛 {@link io.jsonwebtoken.ExpiredJwtException}（其子类），由调用方映射为对应 ErrorCode。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Component
public final class JwtUtils {

    /** claims 键：用户名 */
    public static final String CLAIM_USERNAME = "username";
    /** claims 键：角色 */
    public static final String CLAIM_ROLE = "role";
    /** claims 键：令牌类型 */
    public static final String CLAIM_TYPE = "type";
    /** 令牌类型：访问令牌 */
    public static final String TYPE_ACCESS = "access";
    /** 令牌类型：刷新令牌 */
    public static final String TYPE_REFRESH = "refresh";

    private final JwtProperties props;
    private final SecretKey key;

    public JwtUtils(JwtProperties props) {
        this.props = props;
        this.key = Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 Access Token.
     *
     * @param userId   用户 ID
     * @param username 用户名
     * @param role     角色名（{@link com.library.core.enums.RoleEnum#name()}）
     * @return 已签名的 JWT 字符串
     */
    public String generateAccessToken(long userId, String username, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_ROLE, role)
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(props.getAccessTokenExpiration())))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 生成 Refresh Token 及其 jti.
     *
     * @param userId 用户 ID
     * @return 令牌数据（token 字符串 + jti）
     */
    public RefreshTokenData generateRefreshToken(long userId) {
        String jti = UUID.randomUUID().toString();
        Instant now = Instant.now();
        String token = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_TYPE, TYPE_REFRESH)
                .id(jti)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(props.getRefreshTokenExpiration())))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
        return new RefreshTokenData(token, jti);
    }

    /**
     * 解析并验签 token.
     *
     * @param token JWT 字符串
     * @return claims 载荷
     * @throws io.jsonwebtoken.ExpiredJwtException token 已过期
     * @throws JwtException                        签名无效/格式错误
     */
    public Claims parse(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** 判断 claims 是否为 Access Token */
    public boolean isAccess(Claims claims) {
        return TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class));
    }

    /** 判断 claims 是否为 Refresh Token */
    public boolean isRefresh(Claims claims) {
        return TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class));
    }

    /** 获取 Access Token 有效期（秒） */
    public long getAccessTokenExpiresInSeconds() {
        return props.getAccessTokenExpiration() / 1000;
    }

    /** Refresh Token 数据载体 */
    public record RefreshTokenData(String token, String jti) {
    }
}
