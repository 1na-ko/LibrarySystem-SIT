package com.library.common.constraint;

import com.library.common.annotation.StrongPassword;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * {@link StrongPassword} 校验器.
 * <p>
 * 规则：长度 8-32 位，至少包含大写字母、小写字母、数字、特殊字符（非字母数字）各一个。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, CharSequence> {

    /**
     * 密码强度正则.
     * <ul>
     *   <li>{@code (?=.*[a-z])} 至少一个小写字母</li>
     *   <li>{@code (?=.*[A-Z])} 至少一个大写字母</li>
     *   <li>{@code (?=.*\d)}    至少一个数字</li>
     *   <li>{@code (?=.*[^a-zA-Z0-9])} 至少一个特殊字符（非字母数字）</li>
     *   <li>{@code .{8,32}}    长度 8-32</li>
     * </ul>
     */
    private static final Pattern PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).{8,32}$");

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        if (value == null) {
            // null 由 @NotBlank 负责，此处放行
            return true;
        }
        return PATTERN.matcher(value).matches();
    }
}
