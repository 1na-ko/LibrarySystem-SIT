package com.library.ai.embedding;

import com.library.ai.config.EmbeddingConfig;
import com.library.ai.embedding.dto.EmbeddingResponse;
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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link EmbeddingServiceImpl} 单元测试.
 * <p>
 * 使用 Mockito {@link Answers#RETURNS_DEEP_STUBS} 自动生成 WebClient 链的 Mock。
 * 验证批量拆批逻辑和空输入降级行为。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@DisplayName("EmbeddingServiceImpl")
@ExtendWith(MockitoExtension.class)
class EmbeddingServiceImplTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private WebClient dashscopeWebClient;

    @Mock
    private EmbeddingConfig embeddingConfig;

    private EmbeddingServiceImpl embeddingService;

    private static final int DIM = 1024;

    @BeforeEach
    void setUp() {
        lenient().when(embeddingConfig.getEmbeddingModel()).thenReturn("text-embedding-v3");
        lenient().when(embeddingConfig.getReadTimeout()).thenReturn(Duration.ofSeconds(30));
        lenient().when(embeddingConfig.getMaxBatchSize()).thenReturn(25);
        embeddingService = new EmbeddingServiceImpl(dashscopeWebClient, embeddingConfig);
    }

    /**
     * 构建模拟 Embedding 响应的工具方法.
     */
    private EmbeddingResponse buildMockResponse(List<String> texts) {
        EmbeddingResponse response = new EmbeddingResponse();
        EmbeddingResponse.Output output = new EmbeddingResponse.Output();
        List<EmbeddingResponse.EmbeddingItem> items = new ArrayList<>();
        for (int i = 0; i < texts.size(); i++) {
            EmbeddingResponse.EmbeddingItem item = new EmbeddingResponse.EmbeddingItem();
            item.setTextIndex(i);
            List<Float> vec = new ArrayList<>(Collections.nCopies(DIM, 0.0f));
            vec.set(0, (float) i);
            item.setEmbedding(vec);
            items.add(item);
        }
        output.setEmbeddings(items);
        response.setOutput(output);
        return response;
    }

    @Nested
    @DisplayName("embed — 单文本向量化")
    class Embed {

        @Test
        @DisplayName("正常文本应返回 1024 维向量")
        void shouldReturn1024DimVectorForSingleText() {
            when(dashscopeWebClient.post()
                    .uri(anyString())
                    .bodyValue(any())
                    .retrieve()
                    .onStatus(any(), any())
                    .bodyToMono(EmbeddingResponse.class))
                    .thenReturn(Mono.just(buildMockResponse(List.of("Java编程"))));

            List<Float> vector = embeddingService.embed("Java编程");

            assertThat(vector).hasSize(DIM);
            assertThat(vector.get(0)).isEqualTo(0.0f);
        }

        @Test
        @DisplayName("空文本应返回 1024 维零向量")
        void shouldReturnZeroVectorForBlankText() {
            List<Float> vector = embeddingService.embed("");
            assertThat(vector).hasSize(DIM);
            assertThat(vector.get(0)).isEqualTo(0.0f);
        }

        @Test
        @DisplayName("null 文本应返回 1024 维零向量")
        void shouldReturnZeroVectorForNullText() {
            List<Float> vector = embeddingService.embed(null);
            assertThat(vector).hasSize(DIM);
            assertThat(vector.get(0)).isEqualTo(0.0f);
        }
    }

    @Nested
    @DisplayName("batchEmbed — 批量向量化")
    class BatchEmbed {

        @Test
        @DisplayName("数量未超 max-batch-size 时应单次调用 API")
        void shouldBatchEmbedWithinLimit() {
            int textCount = 20;
            List<String> texts = Collections.nCopies(textCount, "测试文本");
            when(dashscopeWebClient.post()
                    .uri(anyString())
                    .bodyValue(any())
                    .retrieve()
                    .onStatus(any(), any())
                    .bodyToMono(EmbeddingResponse.class))
                    .thenReturn(Mono.just(buildMockResponse(texts)));

            List<List<Float>> results = embeddingService.batchEmbed(texts);

            assertThat(results).hasSize(textCount);
        }

        @Test
        @DisplayName("超过 max-batch-size 时应多次拆批调用")
        void shouldSplitBatchWhenExceedsLimit() {
            when(embeddingConfig.getMaxBatchSize()).thenReturn(5);
            int textCount = 12;
            List<String> texts = Collections.nCopies(textCount, "测试文本");

            when(dashscopeWebClient.post()
                    .uri(anyString())
                    .bodyValue(any())
                    .retrieve()
                    .onStatus(any(), any())
                    .bodyToMono(EmbeddingResponse.class))
                    .thenReturn(Mono.just(buildMockResponse(texts.subList(0, 5))))
                    .thenReturn(Mono.just(buildMockResponse(texts.subList(5, 10))))
                    .thenReturn(Mono.just(buildMockResponse(texts.subList(10, 12))));

            List<List<Float>> results = embeddingService.batchEmbed(texts);

            assertThat(results).hasSize(textCount);
            assertThat(results.get(0)).hasSize(DIM);
            assertThat(results.get(5)).hasSize(DIM);
            assertThat(results.get(10)).hasSize(DIM);
        }

        @Test
        @DisplayName("空列表应返回空结果")
        void shouldReturnEmptyForNullList() {
            assertThat(embeddingService.batchEmbed(null)).isEmpty();
            assertThat(embeddingService.batchEmbed(List.of())).isEmpty();
        }

        @Test
        @DisplayName("全部为空文本时应返回等量零向量不调用 API")
        void shouldReturnZeroVectorsForAllBlankTexts() {
            List<String> texts = java.util.Arrays.asList("", "   ", null);

            List<List<Float>> results = embeddingService.batchEmbed(texts);

            assertThat(results).hasSize(3);
            assertThat(results.get(0)).hasSize(DIM);
            assertThat(results.get(1)).hasSize(DIM);
            assertThat(results.get(2)).hasSize(DIM);
            verify(dashscopeWebClient, times(0)).post();
        }

        @Test
        @DisplayName("混合空文本与非空文本时应保持原始顺序")
        void shouldPreserveOriginalOrderWithMixedEmptyAndNonEmpty() {
            List<String> texts = List.of("文本A", "", "文本B");
            when(dashscopeWebClient.post()
                    .uri(anyString())
                    .bodyValue(any())
                    .retrieve()
                    .onStatus(any(), any())
                    .bodyToMono(EmbeddingResponse.class))
                    .thenReturn(Mono.just(buildMockResponse(List.of("文本A", "文本B"))));

            List<List<Float>> results = embeddingService.batchEmbed(texts);

            assertThat(results).hasSize(3);
            assertThat(results.get(0).get(0)).isEqualTo(0.0f);
            assertThat(results.get(1).get(0)).isEqualTo(0.0f); // "" → 零向量
            assertThat(results.get(2).get(0)).isEqualTo(1.0f);
        }
    }
}
