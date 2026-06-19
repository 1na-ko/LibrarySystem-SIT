package com.library.android.network.exception;

/**
 * 权限不足异常 — HTTP 403.
 *
 * <p>已登录但当前角色无权访问该端点（@RequireRole / @RequirePermission 拦截）.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public class PermissionDeniedException extends ApiException {
    public PermissionDeniedException(String serverMessage) {
        super(403, serverMessage);
    }
}
