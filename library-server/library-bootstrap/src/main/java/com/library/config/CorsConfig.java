package com.library.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * 跨域配置.
 * <p>
 * 仅暴露 {@link CorsConfigurationSource} 作为单一 CORS 配置来源，由 Spring Security 的
 * {@code .cors()} 注入过滤器链统一处理预检与跨域（{@link com.library.security.config.SecurityConfig}）。
 * <p>
 * 本项目使用 Bearer Header 认证（非 Cookie），故 {@code allowCredentials=false}，
 * 避免与 {@code allowedOriginPatterns=*} 组合形成 CORS 凭据泄露面。生产环境应进一步收紧为具体域名白名单。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Configuration
public class CorsConfig {

    /**
     * CORS 配置源（单一来源，供 Security 过滤器链使用）.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // 开发环境宽松；生产需收紧为具体域名白名单
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        // Bearer Header 认证（非 Cookie），无需允许凭据
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
