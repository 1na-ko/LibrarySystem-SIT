package com.library.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解.
 * <p>
 * 标注于 Controller 方法上，由 {@code OperationLogAspect} 在运行时自动记录操作日志。
 * 日志异步写入 MySQL {@code operation_log} 表，写入失败不影响主流程。
 * <p>
 * 使用示例：
 * <pre>{@code
 * @OperationLog(module = "用户管理", action = "状态变更")
 * @PutMapping("/{id}/status")
 * public Result<Void> updateStatus(...) { }
 * }</pre>
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /**
     * 操作模块，如"用户管理"、"图书编目"。
     */
    String module() default "";

    /**
     * 操作动作，如"状态变更"、"删除图书"。
     */
    String action() default "";

    /**
     * 是否记录请求参数（默认 false，避免敏感信息泄露）。
     * 如需记录，敏感字段（password/token/secret 等）将由切面自动脱敏为 ***。
     */
    boolean logParams() default false;

    /**
     * 是否记录返回结果（默认 false，避免大响应撑爆日志列）。
     */
    boolean logResult() default false;
}
