package com.library.security.aspect;

import com.library.core.enums.RoleEnum;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 角色权限注解（方法级）.
 * <p>
 * 标注于 Spring Bean 的 public 方法上，由 {@link AuthorizationAspect} 校验当前用户角色是否满足。
 * 默认 OR 语义（满足其一即可），{@link #requireAll()} 设为 true 时为 AND。
 * <p>
 * 注意：受 Spring AOP 限制，注解仅对 Bean 的 public 方法、经代理的外部调用生效；
 * 类级标注、自调用、非 public 方法不触发切面，故仅支持方法级（避免类级注解误判为已生效）。
 *
 * <pre>{@code
 * @RequireRole({RoleEnum.LIBRARIAN, RoleEnum.ADMIN})
 * public void approveBorrow(...) { }
 * }</pre>
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {

    /** 允许的角色 */
    RoleEnum[] value();

    /** 是否要求满足全部角色（默认 false=满足其一） */
    boolean requireAll() default false;
}
