package com.library.ai.llm;

import lombok.Getter;

/**
 * LLM 服务不可用异常.
 * <p>
 * 当 DeepSeek API 因网络超时、API Key 无效、配额耗尽、JSON 解析失败等原因不可达时抛出。
 * 此异常继承 {@link RuntimeException} 而非 {@link com.library.common.exception.BizException}，
 * 因为 LLM 不可用是基础设施故障，而非业务规则违反。
 * 调用方应捕获此异常并实施降级策略（如回退到本地模板、规则引擎或 HanLP 关键词匹配）。
 *
 * <p>不可用原因由 {@link #reason} 字段标识，便于调用方按故障类型选择降级路径：
 * <ul>
 *   <li>{@code AUTH_FAILED} — API Key 无效或权限不足（HTTP 401/403）</li>
 *   <li>{@code QUOTA_EXHAUSTED} — 配额耗尽或频率限制（HTTP 429）</li>
 *   <li>{@code SERVER_ERROR} — 服务端内部错误（HTTP 5xx）</li>
 *   <li>{@code NETWORK_ERROR} — 网络连接/超时异常</li>
 *   <li>{@code PARSE_ERROR} — JSON Mode 下 LLM 返回内容无法解析为目标类型</li>
 *   <li>{@code RETRY_EXHAUSTED} — 重试指定次数后仍失败</li>
 *   <li>{@code TIMEOUT} — 读取超时</li>
 * </ul>
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Getter
public class LlmUnavailableException extends RuntimeException {

    /** 不可用原因标识 */
    private final String reason;

    /**
     * 构造仅含消息的异常.
     *
     * @param message 错误描述
     */
    public LlmUnavailableException(String message) {
        super(message);
        this.reason = "UNKNOWN";
    }

    /**
     * 构造含消息和原因的异常.
     *
     * @param message 错误描述
     * @param reason  不可用原因标识
     */
    public LlmUnavailableException(String message, String reason) {
        super(message);
        this.reason = reason;
    }

    /**
     * 构造含消息和原始异常的异常.
     *
     * @param message 错误描述
     * @param cause   原始异常
     */
    public LlmUnavailableException(String message, Throwable cause) {
        super(message, cause);
        this.reason = "UNKNOWN";
    }

    /**
     * 构造含消息、原始异常和原因的异常.
     *
     * @param message 错误描述
     * @param cause   原始异常
     * @param reason  不可用原因标识
     */
    public LlmUnavailableException(String message, Throwable cause, String reason) {
        super(message, cause);
        this.reason = reason;
    }
}
