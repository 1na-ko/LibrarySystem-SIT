package com.library.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记公开接口（无需认证）.
 * <p>
 * <b>重要</b>：此注解为代码级文档标记，<b>不</b>在运行时被任何过滤器/AOP 读取。
 * 实际的访问控制由 {@code SecurityConfig.requestMatchers(...).permitAll()} 全权负责。
 * 新增公开接口时，<b>必须同步</b>在 SecurityConfig 中添加白名单路径，仅加此注解无效。
 * <p>
 * 使用示例：
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
