package com.library.android.network.exception;

/**
 * API 调用异常基类 — 所有 HTTP 业务异常的根类型.
 *
 * <p>用于在 RxJava {@code onError} 链上区分业务失败（携带后端 code/message）
 * 与本地代码缺陷。子类按 HTTP 状态码精细化分类，UI 层通过 {@code instanceof}
 * 给出贴合的提示文案.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public class ApiException extends RuntimeException {

    /** HTTP 状态码（401/403/409/422/503/...）. */
    private final int httpCode;

    /** 后端业务消息（已尝试从 errorBody 解析 Result.message，可能为 null）. */
    private final String serverMessage;

    public ApiException(int httpCode, String serverMessage) {
        super("HTTP " + httpCode + (serverMessage != null ? ": " + serverMessage : ""));
        this.httpCode = httpCode;
        this.serverMessage = serverMessage;
    }

    public ApiException(int httpCode, String serverMessage, Throwable cause) {
        super("HTTP " + httpCode + (serverMessage != null ? ": " + serverMessage : ""), cause);
        this.httpCode = httpCode;
        this.serverMessage = serverMessage;
    }

    public int getHttpCode() {
        return httpCode;
    }

    public String getServerMessage() {
        return serverMessage;
    }
}
