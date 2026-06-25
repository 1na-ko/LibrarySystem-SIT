package com.library.android.network.exception;

/**
 * 资源未找到异常 — HTTP 404.
 *
 * <p>典型场景：资源已被删除、路由配置错误、ID 不存在.
 * UI 层应展示"资源不存在或已被删除"等友好文案，避免泄露 URL 信息.
 *
 * @since 1.0.0
 */
public class NotFoundException extends ApiException {
    public NotFoundException(String serverMessage) {
        super(404, serverMessage);
    }
}
