package com.library.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 认证/授权异常处理器单元测试.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@DisplayName("认证/授权 JSON 异常处理器")
class SecurityHandlersTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("未认证应返回 401 + UNAUTHORIZED 消息")
    void shouldReturn401WhenUnauthenticated() throws Exception {
        JsonAuthenticationEntryPoint entry = new JsonAuthenticationEntryPoint(objectMapper);
        MockHttpServletResponse response = new MockHttpServletResponse();

        entry.commence(new MockHttpServletRequest(), response,
                new BadCredentialsException("no token"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("请先登录");
    }

    @Test
    @DisplayName("权限不足应返回 403 + FORBIDDEN 消息")
    void shouldReturn403WhenForbidden() throws Exception {
        JsonAccessDeniedHandler handler = new JsonAccessDeniedHandler(objectMapper);
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(new MockHttpServletRequest(), response,
                new AccessDeniedException("forbidden"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("权限不足");
    }
}
