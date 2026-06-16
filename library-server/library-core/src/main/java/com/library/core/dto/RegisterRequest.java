package com.library.core.dto;

import com.library.common.annotation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 注册请求 DTO.
 * <p>
 * 对应 OpenAPI {@code RegisterRequest} schema。校验规则与契约一致：
 * username 5-20 / password 强密码 / realName ≤50 / email 合法 / phone 中国手机号（可选）。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    /** 学号/工号（5-20 位） */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 5, max = 20, message = "用户名长度须为 5-20 位")
    private String username;

    /** 密码（8-32 位含大小写字母+数字+特殊字符） */
    @NotBlank(message = "密码不能为空")
    @StrongPassword
    private String password;

    /** 真实姓名 */
    @NotBlank(message = "真实姓名不能为空")
    @Size(max = 50, message = "真实姓名长度不能超过 50")
    private String realName;

    /** 邮箱 */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    /** 手机号（可选，须符合中国大陆手机号格式） */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
}
