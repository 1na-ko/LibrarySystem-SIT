package com.library.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.common.exception.ErrorCode;
import com.library.common.result.Result;
import com.library.security.context.LoginUser;
import com.library.security.context.SecurityUtils;
import com.library.security.ratelimit.RateLimitProperties;
import com.library.security.ratelimit.RateLimitResult;
import com.library.security.ratelimit.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 限流过滤器（Redis 令牌桶）.
 * <p>
 * 位于 JwtAuthenticationFilter 之后，按路径类别与认证状态分流：
 * <ul>
 *   <li>{@code /auth/login}、{@code /auth/register} → 按 IP 防爆破（默认 20/min）</li>
 *   <li>已认证请求 → 按 userId（默认 100/min）</li>
 *   <li>匿名请求 → 按 IP（默认 100/min）</li>
 *   <li>{@code /health}、swagger、api-docs、OPTIONS 预检 → 不限流</li>
 * </ul>
 * 超限返回 429 + X-RateLimit-* / Retry-After Header。
 * <p>
 * 路径判断使用 {@code getServletPath()}（已剥离 context-path），与 {@code SecurityConfig}
 * 的 requestMatchers 保持同一抽象，避免 context-path 变更导致规则漂移。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getServletPath();
        if (shouldNotRateLimit(path, request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        BucketConfig bucket = resolveBucket(path, request);
        RateLimitResult result = rateLimitService.tryAcquire(bucket.key(), bucket.capacity(), bucket.refillPerSec());

        // 始终回写限流 Header（API 契约要求）
        response.setHeader("X-RateLimit-Limit", String.valueOf(result.capacity()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, result.remaining())));
        response.setHeader("X-RateLimit-Reset", String.valueOf(result.resetEpochSecond()));

        if (!result.allowed()) {
            long nowSec = System.currentTimeMillis() / 1000;
            long retryAfter = Math.max(1, result.resetEpochSecond() - nowSec);
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(Result.error(ErrorCode.RATE_LIMITED)));
            return;
        }
        chain.doFilter(request, response);
    }

    /** 白名单：健康检查、文档、CORS 预检不限流 */
    private boolean shouldNotRateLimit(String path, String method) {
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }
        return path.startsWith("/health")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/api-docs")
                || path.startsWith("/v3/api-docs");
    }

    private BucketConfig resolveBucket(String path, HttpServletRequest request) {
        // 防爆破端点（按 IP 收紧）
        if (path.startsWith("/auth/login")) {
            RateLimitProperties.Bucket b = properties.getLogin();
            return new BucketConfig("login:" + clientIp(request), b.getCapacity(), b.refillPerSec());
        }
        if (path.startsWith("/auth/register")) {
            RateLimitProperties.Bucket b = properties.getRegister();
            return new BucketConfig("register:" + clientIp(request), b.getCapacity(), b.refillPerSec());
        }
        // 其余：已认证按 userId，匿名按 IP
        LoginUser user = SecurityUtils.getCurrentUser();
        if (user != null) {
            RateLimitProperties.Bucket b = properties.getAuthenticated();
            return new BucketConfig("auth:" + user.getUserId(), b.getCapacity(), b.refillPerSec());
        }
        RateLimitProperties.Bucket b = properties.getAnonymous();
        return new BucketConfig("anon:" + clientIp(request), b.getCapacity(), b.refillPerSec());
    }

    /**
     * 提取客户端 IP.
     * <p>
     * 直接使用 Servlet 容器规范化后的 {@code getRemoteAddr()}，<b>不</b>自行解析
     * {@code X-Forwarded-For} / {@code X-Real-IP}——这些头可被客户端伪造，攻击者可
     * 携带随机伪造头分散限流桶，绕过登录防爆破。
     * <p>
     * 生产环境部署在反向代理之后时，应启用 {@code server.forward-headers-strategy: native}
     * 并配置 {@code server.tomcat.remoteip.trusted-proxies}，由 Tomcat RemoteIpValve 仅在
     * 请求来自受信代理时才采信 XFF，将其规范化进 {@code getRemoteAddr()}。
     */
    private String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    /** 桶配置载体 */
    private record BucketConfig(String key, int capacity, double refillPerSec) {
    }
}
