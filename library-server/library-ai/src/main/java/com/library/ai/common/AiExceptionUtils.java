package com.library.ai.common;

/**
 * AI 模块异常处理工具类.
 * <p>
 * 提供 LLM / Embedding 服务共用的异常分类逻辑，避免在两个 ServiceImpl 中重复。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public final class AiExceptionUtils {

    private AiExceptionUtils() {
        // 工具类，禁止实例化
    }

    /**
     * 按 HTTP 状态码分类失败原因，用于 {@code LlmUnavailableException} 的 reason 字段.
     *
     * @param statusCode HTTP 响应状态码
     * @return 失败原因分类（AUTH_FAILED / QUOTA_EXHAUSTED / SERVER_ERROR / CLIENT_ERROR）
     */
    public static String categorizeReason(int statusCode) {
        if (statusCode == 401 || statusCode == 403) {
            return "AUTH_FAILED";
        }
        if (statusCode == 429) {
            return "QUOTA_EXHAUSTED";
        }
        if (statusCode >= 500) {
            return "SERVER_ERROR";
        }
        return "CLIENT_ERROR";
    }
}
