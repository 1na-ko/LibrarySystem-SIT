package com.library.ai.llm;

/**
 * LLM 服务接口.
 * <p>
 * 封装 DeepSeek API 的调用，提供文本生成与 JSON 结构化输出能力。
 * 所有方法均为同步调用（内部使用 WebClient + block），调用方应捕获
 * {@link LlmUnavailableException} 以实施降级策略。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public interface LlmService {

    /**
     * 发送 Prompt 并返回文本回复.
     *
     * @param prompt 用户提示词（非空）
     * @return LLM 生成的文本回复
     * @throws LlmUnavailableException 当 API 不可达、超时、重试耗尽时
     */
    String chat(String prompt);

    /**
     * 发送 Prompt 并返回 JSON 反序列化对象（JSON Mode）.
     * <p>
     * 请求中会设置 {@code response_format.type = "json_object"}，
     * 并追加系统指令确保 LLM 输出合法 JSON。
     *
     * @param prompt       用户提示词（非空）
     * @param responseType 目标反序列化类型
     * @param <T>          响应类型
     * @return 反序列化后的 Java 对象
     * @throws LlmUnavailableException 当 API 不可达或 JSON 解析失败时
     */
    <T> T chat(String prompt, Class<T> responseType);
}
