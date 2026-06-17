package com.library.security.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.common.result.Result;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;

/**
 * Security 模块 JSON 错误响应工具.
 * <p>
 * 消除 JwtAuthenticationFilter、RateLimitFilter、JsonAccessDeniedHandler、
 * JsonAuthenticationEntryPoint 四处重复的 JSON 响应写回样板代码。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public final class SecurityResponseUtil {

    private SecurityResponseUtil() {
        // 工具类，禁止实例化
    }

    /**
     * 向 HttpServletResponse 写入 JSON 格式错误响应.
     *
     * @param response    HttpServletResponse
     * @param status      HTTP 状态码
     * @param result      业务 Result 对象
     * @param objectMapper Jackson ObjectMapper
     * @throws IOException 写入失败
     */
    public static void writeJsonError(HttpServletResponse response,
                                       HttpStatus status,
                                       Result<?> result,
                                       ObjectMapper objectMapper) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
