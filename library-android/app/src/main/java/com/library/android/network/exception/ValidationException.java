package com.library.android.network.exception;

/**
 * 参数校验失败异常 — HTTP 400 / 422（@Valid 校验失败 / 业务字段非法）.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public class ValidationException extends ApiException {
    public ValidationException(int httpCode, String serverMessage) {
        super(httpCode, serverMessage);
    }
}
