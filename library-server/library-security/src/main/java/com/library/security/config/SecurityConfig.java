package com.library.security.config;

import com.library.security.filter.JwtAuthenticationFilter;
import com.library.security.filter.RateLimitFilter;
import com.library.security.handler.JsonAccessDeniedHandler;
import com.library.security.handler.JsonAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Spring Security 配置.
 * <p>
 * 无状态 JWT：禁用 CSRF + STATELESS Session；注册 JwtAuthenticationFilter 与 RateLimitFilter
 * （顺序：Jwt → RateLimit，RateLimit 从 SecurityContext 取 userId）；公开端点放行，
 * 其余需认证；认证/授权异常输出统一 {@code Result} JSON。
 * <p>
 * 注意：{@code context-path=/api/v1} 由 Spring Security 6 默认 MvcMatcher 自动剥离，
 * requestMatchers 路径不带 {@code /api/v1} 前缀。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitFilter rateLimitFilter;
    private final JsonAuthenticationEntryPoint authenticationEntryPoint;
    private final JsonAccessDeniedHandler accessDeniedHandler;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        // 公开认证端点
                        .requestMatchers("/auth/login", "/auth/register", "/auth/refresh").permitAll()
                        // 健康检查、监控指标、API 文档
                        .requestMatchers("/health", "/health/**",
                                "/prometheus",
                                "/swagger-ui/**", "/swagger-ui.html",
                                "/api-docs", "/api-docs/**", "/v3/api-docs/**").permitAll()
                        // CORS 预检放行
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 其余端点需认证（含 /auth/logout）
                        .anyRequest().authenticated())
                // Jwt 认证 → 限流（顺序：Jwt 在前解析 token 设 context，RateLimit 随后按 userId/IP 限流）
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(rateLimitFilter, JwtAuthenticationFilter.class);
        return http.build();
    }

    /**
     * 密码编码器（BCrypt cost=12，架构文档 §9.3）.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
