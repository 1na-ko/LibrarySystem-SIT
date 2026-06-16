package com.library.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.security.ratelimit.RateLimitProperties;
import com.library.security.ratelimit.RateLimitResult;
import com.library.security.ratelimit.RateLimitService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RateLimitFilter 单元测试.
 * <p>
 * 路径通过 {@code setServletPath} 模拟（剥离 context-path 后的路径，与生产 getServletPath 一致）。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitFilter 限流过滤器")
class RateLimitFilterTest {

    @Mock
    private RateLimitService rateLimitService;

    private RateLimitFilter filter;
    private RateLimitProperties properties;

    @BeforeEach
    void setUp() {
        properties = new RateLimitProperties();
        filter = new RateLimitFilter(rateLimitService, properties, new ObjectMapper());
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletRequest request(String method, String servletPath) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, servletPath);
        request.setServletPath(servletPath);
        return request;
    }

    @Test
    @DisplayName("健康检查路径应直接放行不限流")
    void shouldSkipHealthCheck() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request("GET", "/health"), response, chain);

        assertThat(chain.getRequest()).isNotNull();
        verify(rateLimitService, org.mockito.Mockito.never())
                .tryAcquire(eq(""), anyInt(), anyDouble());
    }

    @Test
    @DisplayName("OPTIONS 预检应放行不限流")
    void shouldSkipOptionsPreflight() throws Exception {
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request("OPTIONS", "/books"), new MockHttpServletResponse(), chain);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("login 端点应按 IP 防爆破桶限流")
    void shouldUseLoginBucketByIp() throws Exception {
        MockHttpServletRequest req = request("POST", "/auth/login");
        req.setRemoteAddr("1.2.3.4");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(rateLimitService.tryAcquire(eq("login:1.2.3.4"), anyInt(), anyDouble()))
                .thenReturn(new RateLimitResult(true, 19, 1000L, 20));

        filter.doFilter(req, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("20");
        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("19");
    }

    @Test
    @DisplayName("超限应返回 429 与 Retry-After Header")
    void shouldReturn429WhenRateLimited() throws Exception {
        MockHttpServletRequest req = request("POST", "/auth/login");
        req.setRemoteAddr("1.2.3.4");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(rateLimitService.tryAcquire(eq("login:1.2.3.4"), anyInt(), anyDouble()))
                .thenReturn(new RateLimitResult(false, 0, Integer.MAX_VALUE, 20));

        filter.doFilter(req, response, chain);

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isNotNull();
        assertThat(chain.getRequest()).isNull();
        assertThat(response.getContentAsString()).contains("请求过于频繁");
    }

    @Test
    @DisplayName("已认证请求应按 userId 限流")
    void shouldUseUserIdBucketWhenAuthenticated() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        setAuthenticated(42L);

        when(rateLimitService.tryAcquire(eq("auth:42"), anyInt(), anyDouble()))
                .thenReturn(new RateLimitResult(true, 99, 1000L, 100));

        filter.doFilter(request("GET", "/books/search"), response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("100");
    }

    @Test
    @DisplayName("匿名请求应按 IP 限流（100/min）")
    void shouldUseAnonymousBucketByIp() throws Exception {
        MockHttpServletRequest req = request("POST", "/auth/refresh");
        req.setRemoteAddr("9.9.9.9");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(rateLimitService.tryAcquire(eq("anon:9.9.9.9"), anyInt(), anyDouble()))
                .thenReturn(new RateLimitResult(true, 99, 1000L, 100));

        filter.doFilter(req, response, chain);

        assertThat(chain.getRequest()).isNotNull();
    }

    private void setAuthenticated(long userId) {
        com.library.security.context.LoginUser user =
                new com.library.security.context.LoginUser(userId, "u",
                        com.library.core.enums.RoleEnum.STUDENT,
                        com.library.core.enums.UserStatusEnum.ACTIVE, java.util.List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }
}
