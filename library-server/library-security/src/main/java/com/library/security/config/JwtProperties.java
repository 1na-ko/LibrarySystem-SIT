package com.library.security.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * JWT 配置属性.
 * <p>
 * 绑定 {@code application.yml} 中 {@code jwt.*} 配置键。
 * 启动时校验密钥长度（HS256 要求 ≥ 32 字节），不满足则 fail-fast。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** HS256 签名密钥（须 ≥ 32 字节，建议由 {@code openssl rand -base64 48} 生成）。
     *  默认值仅开发环境兜底（≥32 字节通过 @PostConstruct 校验）；生产环境必须由 JWT_SECRET 环境变量注入强随机密钥 */
    private String secret = "library-system-dev-jwt-secret-replace-in-production-2026";

    /** Access Token 有效期（毫秒），默认 2 小时 */
    private long accessTokenExpiration = 7_200_000L;

    /** Refresh Token 有效期（毫秒），默认 7 天 */
    private long refreshTokenExpiration = 604_800_000L;

    /**
     * 启动校验：密钥必须满足 HS256 最小长度（256 bit = 32 字节）.
     * <p>
     * 生产环境通过环境变量 {@code JWT_SECRET} 注入；开发环境请在本地 {@code .env} 设置。
     */
    @PostConstruct
    void validate() {
        int len = secret == null ? 0 : secret.getBytes(StandardCharsets.UTF_8).length;
        if (len < 32) {
            throw new IllegalStateException(
                    "jwt.secret 必须 >= 32 字节（HS256 要求），当前 " + len
                            + " 字节。请设置环境变量 JWT_SECRET，生成命令：openssl rand -base64 48");
        }
    }
}
