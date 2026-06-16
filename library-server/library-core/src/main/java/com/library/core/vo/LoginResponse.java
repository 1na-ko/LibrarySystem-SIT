package com.library.core.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应 VO.
 * <p>
 * 对应 OpenAPI {@code LoginResponse} schema，包含令牌对与用户信息。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    /** 访问令牌（2h） */
    private String accessToken;

    /** 刷新令牌（7d） */
    private String refreshToken;

    /** 令牌类型，固定 "Bearer" */
    @Builder.Default
    private String tokenType = "Bearer";

    /** Access Token 有效期（秒） */
    private int expiresIn;

    /** 用户信息 */
    private UserProfile user;
}
