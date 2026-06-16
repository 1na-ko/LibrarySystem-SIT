package com.library.security.aspect;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 细粒度权限注解（方法级）.
 * <p>
 * 权限标识由 {@link RolePermissionMapper} 按角色派生。
 * 默认 OR 语义，{@link #requireAll()} 设为 true 时为 AND。
 * <p>
 * 注意：同 {@link RequireRole}，仅对 Bean 的 public 方法、经代理的外部调用生效。
 *
 * <pre>{@code
 * @RequirePermission("kg:admin")
 * public void rebuildGraph(...) { }
 * }</pre>
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {

    /** 要求的权限标识（如 "kg:admin"、"book:catalog"） */
    String[] value();

    /** 是否要求满足全部权限（默认 false=满足其一） */
    boolean requireAll() default false;
}
