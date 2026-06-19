package com.library.android.network.exception;

/**
 * 服务不可用异常 — HTTP 503 / 502 / 504（后端宕机或下游故障）.
 *
 * <p>UI 层应展示"服务暂时不可用，请稍后重试"+ 重试按钮.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public class ServiceUnavailableException extends ApiException {
    public ServiceUnavailableException(int httpCode, String serverMessage) {
        super(httpCode, serverMessage);
    }
}
