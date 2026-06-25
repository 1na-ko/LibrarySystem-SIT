package com.library.ai.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.ai.common.AiExceptionUtils;
import com.library.ai.config.LlmConfig;
import com.library.ai.llm.dto.LlmChatRequest;
import com.library.ai.llm.dto.LlmChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;

/**
 * LLM 服务实现.
 * <p>
 * 通过 WebClient 调用 DeepSeek Chat Completions API（OpenAI 兼容），
 * 支持文本生成和 JSON Mode 结构化输出。内置 reactor-retry 指数退避重试
 * （最多 {@code ai.deepseek.max-retries} 次，默认 2 次）。
 * <p>
 * 仅在 {@code ai.deepseek.api-key} 非空时创建 Bean，否则该 Bean 不存在。
 * 调用方应使用 {@code @Autowired(required = false)} 或 {@code Optional<LlmService>}
 * 注入以适配无 API Key 环境。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${ai.deepseek.api-key:}')")
public class LlmServiceImpl implements LlmService {

    private final WebClient deepseekWebClient;
    private final LlmConfig llmConfig;
    private final ObjectMapper objectMapper;

    @Override
    public String chat(String prompt) {
        LlmChatRequest request = buildRequest(prompt, false);
        LlmChatResponse response = executeWithRetry(request);
        String content = response.firstContent();
        log.info("DeepSeek chat 完成: prompt={}, responseLength={}",
                prompt.length() > 50 ? prompt.substring(0, 50) + "..." : prompt,
                content.length());
        return content;
    }

    @Override
    public <T> T chat(String prompt, Class<T> responseType) {
        LlmChatRequest request = buildRequest(prompt, true);
        LlmChatResponse response = executeWithRetry(request);
        String jsonContent = response.firstContent();

        // 剥离可能的 markdown 代码块标记
        String cleanJson = stripMarkdownCodeBlock(jsonContent);

        try {
            T result = objectMapper.readValue(cleanJson, responseType);
            log.info("DeepSeek chat (JSON Mode) 完成: prompt={}, responseType={}",
                    prompt.length() > 50 ? prompt.substring(0, 50) + "..." : prompt,
                    responseType.getSimpleName());
            return result;
        } catch (JsonProcessingException e) {
            log.error("LLM JSON 输出解析失败: responseType={}, rawContent={}",
                    responseType.getSimpleName(),
                    jsonContent.length() > 200 ? jsonContent.substring(0, 200) + "..." : jsonContent, e);
            throw new LlmUnavailableException(
                    "LLM JSON 输出解析失败: " + e.getMessage(), e, "PARSE_ERROR");
        }
    }

    @Override
    public Flux<String> chatStream(String prompt) {
        // 流式模式：stream=true，DeepSeek 返回 text/event-stream，逐 chunk 推送 delta.content
        LlmChatRequest request = buildRequest(prompt, false);
        request.setStream(true);

        return deepseekWebClient.post()
                .uri("/v1/chat/completions")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
                .map(ServerSentEvent::data)
                .filter(data -> data != null && !data.isBlank() && !"[DONE]".equals(data.trim()))
                .map(this::extractDeltaContent)
                .filter(s -> s != null && !s.isEmpty())
                .doOnError(e -> log.warn("DeepSeek 流式调用失败: {}", e.getMessage()))
                .onErrorResume(e -> Flux.empty());  // 流式失败静默结束（调用方应有降级文案）
    }

    /**
     * 从 DeepSeek stream chunk 中提取增量 token.
     * <p>
     * chunk 格式: {@code {"choices":[{"delta":{"content":"根"}}]}}
     *
     * @param data SSE event 的 data 字段（JSON 字符串）
     * @return 增量 token；无 content 或解析失败时返回空串（Reactor .map 不允许返回 null，
     *         空串由调用方 filter 过滤）
     */
    private String extractDeltaContent(String data) {
        try {
            JsonNode node = objectMapper.readTree(data);
            JsonNode content = node.path("choices").path(0).path("delta").path("content");
            return content.asText("");
        } catch (Exception e) {
            log.debug("解析 DeepSeek stream chunk 失败: {}", data);
            return "";
        }
    }

    /**
     * 构建 Chat Completions 请求.
     *
     * @param prompt   用户提示词
     * @param jsonMode 是否启用 JSON Mode
     * @return 请求 DTO
     */
    private LlmChatRequest buildRequest(String prompt, boolean jsonMode) {
        var messages = new ArrayList<LlmChatRequest.Message>();

        if (jsonMode) {
            messages.add(LlmChatRequest.Message.builder()
                    .role("system")
                    .content("You must respond with valid JSON only. No markdown code blocks, no explanation outside the JSON.")
                    .build());
        }

        messages.add(LlmChatRequest.Message.builder()
                .role("user")
                .content(prompt)
                .build());

        return LlmChatRequest.builder()
                .model(llmConfig.getModel())
                .messages(messages)
                .temperature(llmConfig.getTemperature())
                .maxTokens(llmConfig.getMaxTokens())
                .stream(false)
                .responseFormat(jsonMode
                        ? LlmChatRequest.ResponseFormat.builder().type("json_object").build()
                        : null)
                .build();
    }

    /**
     * 执行 HTTP 请求并含重试逻辑.
     *
     * @param request 请求体
     * @return API 响应
     */
    private LlmChatResponse executeWithRetry(LlmChatRequest request) {
        return deepseekWebClient.post()
                .uri("/v1/chat/completions")
                .bodyValue(request)
                .retrieve()
                .onStatus(
                        status -> status.isError(),
                        resp -> resp.bodyToMono(String.class)
                                .map(body -> new LlmUnavailableException(
                                        "DeepSeek API 返回错误: " + resp.statusCode() + " - " + body,
                                        categorizeReason(resp.statusCode().value()))))
                .bodyToMono(LlmChatResponse.class)
                .onErrorMap(IOException.class, e ->
                        new LlmUnavailableException("DeepSeek API 网络错误: " + e.getMessage(), e, "NETWORK_ERROR"))
                .retryWhen(Retry.backoff(llmConfig.getMaxRetries(), Duration.ofSeconds(1))
                        .maxBackoff(Duration.ofSeconds(8))
                        .filter(throwable -> {
                            // 跳过永久性错误的重试（无意义且浪费配额窗口）：
                            // AUTH_FAILED(401/403) / QUOTA_EXHAUSTED(429) / CLIENT_ERROR(400/404 等请求格式错误)
                            if (throwable instanceof LlmUnavailableException e) {
                                String reason = e.getReason();
                                return !"AUTH_FAILED".equals(reason)
                                        && !"QUOTA_EXHAUSTED".equals(reason)
                                        && !"CLIENT_ERROR".equals(reason);
                            }
                            return true; // 网络等临时错误继续重试
                        })
                        .doBeforeRetry(rs -> log.warn("DeepSeek API 调用重试: 第 {} 次, 失败原因: {}",
                                rs.totalRetries() + 1, rs.failure().getMessage()))
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                new LlmUnavailableException(
                                        "DeepSeek API 重试 " + llmConfig.getMaxRetries() + " 次后仍失败",
                                        retrySignal.failure(), "RETRY_EXHAUSTED")))
                .block(llmConfig.getReadTimeout()
                        .multipliedBy(llmConfig.getMaxRetries() + 1)
                        .plusSeconds(20));
    }

    /**
     * 按 HTTP 状态码分类失败原因（委托 AiExceptionUtils）.
     */
    private String categorizeReason(int statusCode) {
        return AiExceptionUtils.categorizeReason(statusCode);
    }

    /**
     * 剥离 LLM 输出中可能包裹的 markdown 代码块标记.
     * <p>
     * DeepSeek JSON Mode 有时仍会在输出外包裹 ```json ... ```，
     * 此处做防御性剥离，确保 JSON 解析器拿到纯净内容。
     */
    private String stripMarkdownCodeBlock(String content) {
        if (content == null || content.isBlank()) {
            return content;
        }
        String trimmed = content.trim();
        // 剥离开头的 ```json / ```（正则兼容单行 ```json{...}``` 无换行的边界情况）
        trimmed = trimmed.replaceAll("^```(?:[a-zA-Z]+)?\\s*", "");
        // 剥离结尾的 ```
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }
}
