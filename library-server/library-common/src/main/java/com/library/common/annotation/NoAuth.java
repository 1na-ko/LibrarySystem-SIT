package com.library.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记公开接口（无需认证）.
 * <p>
 * 加此注解的 Controller 方法会被 Spring Security 过滤器链放行。
 * 配合阶段 1 的 JWT 过滤器使用。
 *
 * <pre>{@code
 * @NoAuth
 * @PostMapping("/auth/login")
 * public Result<LoginResponse> login(...) { }
 * }</pre>
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NoAuth {
}
