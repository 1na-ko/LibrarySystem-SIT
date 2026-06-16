package com.library.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.common.exception.ErrorCode;
import com.library.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 权限不足处理器.
 * <p>
 * 当已认证用户访问无权限端点时，Spring Security 触发此处理器，
 * 返回 403 + 统一 {@link Result} 格式（{@link ErrorCode#FORBIDDEN}）。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.error(ErrorCode.FORBIDDEN)));
    }
}
