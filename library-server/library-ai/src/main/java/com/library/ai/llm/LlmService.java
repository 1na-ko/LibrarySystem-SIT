package com.library.ai.llm;

import reactor.core.publisher.Flux;

/**
 * LLM 服务接口.
 * <p>
 * 封装 DeepSeek API 的调用，提供文本生成与 JSON 结构化输出能力。
 * 同步方法（{@link #chat(String)} / {@link #chat(String, Class)}）内部使用 WebClient + block，
 * 调用方应捕获 {@link LlmUnavailableException} 以实施降级策略。
 * <p>
 * {@link #chatStream(String)} 提供 SSE 流式输出，逐 token 返回，适用于前端"打字机"效果场景。
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

    /**
     * 发送 Prompt 并以 SSE 流式逐 token 返回文本（DeepSeek stream=true）.
     * <p>
     * 每个 Flux 元素是一个增量 token（delta content），调用方可逐 token 推送至前端实现
     * "打字机"效果。流式输出避免长文本生成的整体等待，显著改善用户感知延迟。
     * <p>
     * 注意：流式模式不内置重试（流一旦开始无法中途重试），网络中断由 Flux 的 onError 信号传递，
     * 调用方应订阅 onError 实施降级（如切换模板文案）。
     *
     * @param prompt 用户提示词（非空）
     * @return 增量 token 流（可能为空 Flux，当 LLM 不可用时通过 onError 通知）
     */
    Flux<String> chatStream(String prompt);
}

