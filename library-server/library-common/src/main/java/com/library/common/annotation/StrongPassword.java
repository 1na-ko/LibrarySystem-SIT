package com.library.common.annotation;

import com.library.common.constraint.StrongPasswordValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 密码强度校验注解.
 * <p>
 * 要求密码长度 8-32 位，且至少包含大写字母、小写字母、数字、特殊字符各一个。
 * 特殊字符定义为任意非字母数字字符（含下划线、标点、符号）。
 * <p>
 * 注意：{@code null} 值视为有效（由 {@code @NotBlank} 负责非空校验），
 * 与 Bean Validation 惯例保持一致，避免校验职责重叠。
 *
 * <pre>{@code
 * @NotBlank
 * @StrongPassword
 * private String password;
 * }</pre>
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
public @interface StrongPassword {

    /** 校验失败提示信息 */
    String message() default "密码必须为 8-32 位，且至少包含大写字母、小写字母、数字和特殊字符各一个";

    /** 分组校验 */
    Class<?>[] groups() default {};

    /** 载荷 */
    Class<? extends Payload>[] payload() default {};
}
