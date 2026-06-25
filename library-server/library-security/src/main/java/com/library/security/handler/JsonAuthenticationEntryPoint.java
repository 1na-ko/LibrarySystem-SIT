package com.library.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.common.exception.ErrorCode;
import com.library.common.result.Result;
import com.library.security.util.SecurityResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 未认证访问受保护资源的处理器.
 * <p>
 * 当请求未携带有效 Token 访问需认证端点时，Spring Security 触发此处理器，
 * 返回 401 + 统一 {@link Result} 格式（{@link ErrorCode#UNAUTHORIZED}）。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        SecurityResponseUtil.writeJsonError(response, HttpStatus.UNAUTHORIZED,
                Result.error(ErrorCode.UNAUTHORIZED), objectMapper);
    }
}
