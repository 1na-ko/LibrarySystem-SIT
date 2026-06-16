package com.library.security.context;

import com.library.common.exception.BizException;
import com.library.common.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全上下文工具.
 * <p>
 * 业务层通过本类获取当前登录用户，避免直接耦合 {@code SecurityContextHolder}。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 获取当前登录用户.
     *
     * @return 已认证用户；未登录或主体非 {@link LoginUser} 时返回 {@code null}
     */
    public static LoginUser getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof LoginUser)) {
            return null;
        }
        return (LoginUser) auth.getPrincipal();
    }

    /**
     * 获取当前登录用户 ID.
     *
     * @return 用户 ID
     * @throws BizException 未登录时抛 {@link ErrorCode#UNAUTHORIZED}
     */
    public static long getCurrentUserId() {
        LoginUser user = getCurrentUser();
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return user.getUserId();
    }
}
