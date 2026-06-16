package com.library.ai.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.ai.config.LlmConfig;
import com.library.ai.llm.dto.LlmChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * {@link LlmServiceImpl} 单元测试.
 * <p>
 * 使用 Mockito {@link Answers#RETURNS_DEEP_STUBS} 自动生成 WebClient 链的 Mock，
 * 避免手动设置泛型敏感的类型层次。不发起真实 HTTP 请求。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@DisplayName("LlmServiceImpl")
@ExtendWith(MockitoExtension.class)
class LlmServiceImplTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private WebClient deepseekWebClient;

    @Mock
    private LlmConfig llmConfig;

    private LlmServiceImpl llmService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        lenient().when(llmConfig.getModel()).thenReturn("deepseek-chat");
        lenient().when(llmConfig.getTemperature()).thenReturn(0.7);
        lenient().when(llmConfig.getMaxTokens()).thenReturn(2048);
        lenient().when(llmConfig.getReadTimeout()).thenReturn(Duration.ofSeconds(60));

        llmService = new LlmServiceImpl(deepseekWebClient, llmConfig, objectMapper);
    }

    /**
     * 构建模拟成功响应的工具方法.
     */
    private LlmChatResponse buildMockResponse(String content) {
        LlmChatResponse response = new LlmChatResponse();
        response.setId("test-id-001");
        LlmChatResponse.Choice choice = new LlmChatResponse.Choice();
        choice.setIndex(0);
        LlmChatResponse.Message message = new LlmChatResponse.Message();
        message.setRole("assistant");
        message.setContent(content);
        choice.setMessage(message);
        response.setChoices(List.of(choice));
        return response;
    }

    @Nested
    @DisplayName("chat — 文本生成")
    class Chat {

        @Test
        @DisplayName("API 成功时应返回非空文本回复")
        void shouldReturnTextResponseWhenApiSucceeds() {
            when(llmConfig.getMaxRetries()).thenReturn(2);
            when(deepseekWebClient.post()
                    .uri(anyString())
                    .bodyValue(any())
                    .retrieve()
                    .onStatus(any(), any())
                    .bodyToMono(LlmChatResponse.class))
                    .thenReturn(Mono.just(buildMockResponse("Spring Boot 是一个用于快速构建Java应用的框架")));

            String result = llmService.chat("用一句话介绍Spring Boot");

            assertThat(result).isNotEmpty();
            assertThat(result).contains("Spring Boot");
        }

        @Test
        @DisplayName("AUTH_FAILED/QUOTA_EXHAUSTED 永久性错误应跳过重试，直接传播原始异常")
        void shouldSkipRetryForPermanentErrorsLikeAuthFailed() {
            when(llmConfig.getMaxRetries()).thenReturn(2); // 虽有重试配额，但 AUTH_FAILED 应被 filter 跳过
            when(deepseekWebClient.post()
                    .uri(anyString())
                    .bodyValue(any())
                    .retrieve()
                    .onStatus(any(), any())
                    .bodyToMono(LlmChatResponse.class))
                    .thenReturn(Mono.error(new LlmUnavailableException(
                            "DeepSeek API 返回错误: 401 UNAUTHORIZED", "AUTH_FAILED")));

            assertThatThrownBy(() -> llmService.chat("测试"))
                    .isInstanceOf(LlmUnavailableException.class)
                    .hasMessageContaining("401")
                    .extracting(ex -> ((LlmUnavailableException) ex).getReason())
                    .isEqualTo("AUTH_FAILED"); // 原始异常直接传播，不被 retry 重包装
        }

        @Test
        @DisplayName("SERVER_ERROR 应被重试，maxRetries=0 时由 onRetryExhaustedThrow 包装为 RETRY_EXHAUSTED")
        void shouldThrowRetryExhaustedWhenServerErrorExhaustsRetry() {
            when(llmConfig.getMaxRetries()).thenReturn(0);
            when(deepseekWebClient.post()
                    .uri(anyString())
                    .bodyValue(any())
                    .retrieve()
                    .onStatus(any(), any())
                    .bodyToMono(LlmChatResponse.class))
                    .thenReturn(Mono.error(new LlmUnavailableException(
                            "DeepSeek API 返回错误: 500 Internal Server Error", "SERVER_ERROR")));

            assertThatThrownBy(() -> llmService.chat("测试"))
                    .isInstanceOf(LlmUnavailableException.class)
                    .hasMessageContaining("重试")
                    .extracting(ex -> ((LlmUnavailableException) ex).getReason())
                    .isEqualTo("RETRY_EXHAUSTED");
        }

        @Test
        @DisplayName("网络异常时 onErrorMap 应映射为 NETWORK_ERROR 后再经 retryWhen")
        void shouldMapNetworkErrorAndThenExhaustRetry() {
            when(llmConfig.getMaxRetries()).thenReturn(0);
            when(deepseekWebClient.post()
                    .uri(anyString())
                    .bodyValue(any())
                    .retrieve()
                    .onStatus(any(), any())
                    .bodyToMono(LlmChatResponse.class))
                    .thenReturn(Mono.error(new IOException("Connection timed out")));

            assertThatThrownBy(() -> llmService.chat("测试"))
                    .isInstanceOf(LlmUnavailableException.class)
                    .hasMessageContaining("重试")
                    // 原始 IOException 被 onErrorMap 转为 NETWORK_ERROR，再被 retryWhen 封装
                    .hasCauseInstanceOf(LlmUnavailableException.class);
        }
    }

    @Nested
    @DisplayName("chat (JSON Mode) — 结构化输出")
    class ChatJsonMode {

        @lombok.Data
        @lombok.NoArgsConstructor
        @lombok.AllArgsConstructor
        static class TestDto {
            private String name;
            private int score;
        }

        @Test
        @DisplayName("JSON Mode 成功时应返回正确反序列化的对象")
        void shouldReturnDeserializedObjectInJsonMode() {
            when(llmConfig.getMaxRetries()).thenReturn(2);
            when(deepseekWebClient.post()
                    .uri(anyString())
                    .bodyValue(any())
                    .retrieve()
                    .onStatus(any(), any())
                    .bodyToMono(LlmChatResponse.class))
                    .thenReturn(Mono.just(buildMockResponse("{\"name\":\"Java\",\"score\":95}")));

            TestDto result = llmService.chat("评估Java的分数", TestDto.class);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Java");
            assertThat(result.getScore()).isEqualTo(95);
        }

        @Test
        @DisplayName("JSON 解析失败时应抛出 LlmUnavailableException 含 PARSE_ERROR")
        void shouldThrowLlmUnavailableWhenJsonParseFails() {
            when(llmConfig.getMaxRetries()).thenReturn(0);
            when(deepseekWebClient.post()
                    .uri(anyString())
                    .bodyValue(any())
                    .retrieve()
                    .onStatus(any(), any())
                    .bodyToMono(LlmChatResponse.class))
                    .thenReturn(Mono.just(buildMockResponse("这不是合法的JSON")));

            assertThatThrownBy(() -> llmService.chat("测试", TestDto.class))
                    .isInstanceOf(LlmUnavailableException.class)
                    .hasMessageContaining("JSON")
                    .extracting(ex -> ((LlmUnavailableException) ex).getReason())
                    .isEqualTo("PARSE_ERROR");
        }

        @Test
        @DisplayName("LLM 输出含 markdown 代码块时仍应成功解析 JSON")
        void shouldStripMarkdownCodeBlockAndParse() {
            when(llmConfig.getMaxRetries()).thenReturn(2);
            String jsonWithMarkdown = "```json\n{\"name\":\"Python\",\"score\":88}\n```";
            when(deepseekWebClient.post()
                    .uri(anyString())
                    .bodyValue(any())
                    .retrieve()
                    .onStatus(any(), any())
                    .bodyToMono(LlmChatResponse.class))
                    .thenReturn(Mono.just(buildMockResponse(jsonWithMarkdown)));

            TestDto result = llmService.chat("评估Python的分数", TestDto.class);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Python");
            assertThat(result.getScore()).isEqualTo(88);
        }
    }
}
