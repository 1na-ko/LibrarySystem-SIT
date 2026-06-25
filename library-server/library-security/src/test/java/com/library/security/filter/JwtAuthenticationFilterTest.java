package com.library.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.core.enums.RoleEnum;
import com.library.security.context.LoginUser;
import com.library.security.jwt.JwtUtils;
import com.library.security.token.TokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
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
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * JwtAuthenticationFilter 单元测试.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter JWT 认证过滤器")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private TokenService tokenService;

    private JwtAuthenticationFilter filter;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        filter = new JwtAuthenticationFilter(jwtUtils, objectMapper, tokenService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("无 Token 应放行且不设置 SecurityContext")
    void shouldPassThroughWhenNoToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("有效 Access Token 应设置 LoginUser 到 SecurityContext")
    void shouldSetAuthenticationWhenValidToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid.token.here");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        Claims claims = org.mockito.Mockito.mock(Claims.class);
        when(claims.getSubject()).thenReturn("123");
        when(claims.get(eq(JwtUtils.CLAIM_USERNAME), eq(String.class))).thenReturn("alice");
        when(claims.get(eq(JwtUtils.CLAIM_ROLE), eq(String.class))).thenReturn("ADMIN");
        when(claims.getIssuedAt()).thenReturn(new java.util.Date(System.currentTimeMillis() - 1000));
        when(jwtUtils.parse("valid.token.here")).thenReturn(claims);
        when(jwtUtils.isAccess(claims)).thenReturn(true);
        // tokenService.isAccessTokenLoggedOut Mock 默认返回 false（未登出），无需显式 stub

        filter.doFilter(request, response, chain);

        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertThat(principal).isInstanceOf(LoginUser.class);
        assertThat(((LoginUser) principal).getUserId()).isEqualTo(123L);
        assertThat(((LoginUser) principal).getRole()).isEqualTo(RoleEnum.ADMIN);
    }

    @Test
    @DisplayName("过期 Token 应返回 401 TOKEN_EXPIRED 且不放行")
    void shouldReturn401WhenExpired() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer expired");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(jwtUtils.parse("expired")).thenThrow(new ExpiredJwtException(null, null, "expired"));

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
        assertThat(response.getContentAsString()).contains("登录已过期");
    }

    @Test
    @DisplayName("无效 Token 应返回 401 TOKEN_INVALID")
    void shouldReturn401WhenInvalid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(jwtUtils.parse("invalid")).thenThrow(new JwtException("bad signature") {
        });

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("令牌无效");
    }

    @Test
    @DisplayName("非 access 类型 Token 应返回 401 TOKEN_INVALID")
    void shouldReturn401WhenTokenTypeMismatch() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer refresh.token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        Claims claims = org.mockito.Mockito.mock(Claims.class);
        when(jwtUtils.parse("refresh.token")).thenReturn(claims);
        when(jwtUtils.isAccess(claims)).thenReturn(false);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("非 Bearer 前缀的 Authorization 应视为无 Token 放行")
    void shouldPassThroughWhenNotBearer() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic abc");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtUtils, org.mockito.Mockito.never()).parse(any());
    }
}
