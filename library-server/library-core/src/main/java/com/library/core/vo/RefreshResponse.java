package com.library.core.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Token 刷新响应 VO.
 * <p>
 * 对应 OpenAPI {@code RefreshResponse} schema，仅返回新令牌对（无 tokenType/user）。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshResponse {

    /** 新访问令牌 */
    private String accessToken;

    /** 新刷新令牌（轮换后旧 RT 失效） */
    private String refreshToken;

    /** 新 Access Token 有效期（秒） */
    private int expiresIn;
}
