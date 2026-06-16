package com.library.ai.embedding;

import com.library.ai.config.EmbeddingConfig;
import com.library.ai.embedding.dto.EmbeddingRequest;
import com.library.ai.embedding.dto.EmbeddingResponse;
import com.library.ai.llm.LlmUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 文本向量化服务实现.
 * <p>
 * 通过 WebClient 调用阿里云百炼 DashScope Embedding API，
 * 将文本转换为 1024 维浮点数向量。支持单条和批量向量化，
 * 批量请求自动按 {@code ai.dashscope.max-batch-size}（默认 25）拆批。
 * <p>
 * 仅在 {@code ai.dashscope.api-key} 非空时创建 Bean。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${ai.dashscope.api-key:}')")
public class EmbeddingServiceImpl implements EmbeddingService {

    private final WebClient dashscopeWebClient;
    private final EmbeddingConfig embeddingConfig;

    private static final String EMBEDDING_PATH = "/api/v1/services/embeddings/text-embedding/text-embedding";
    private static final int EMBEDDING_DIM = 1024;
    /** Embedding API 最大重试次数 */
    private static final int MAX_RETRIES = 2;

    @Override
    public List<Float> embed(String text) {
        if (text == null || text.isBlank()) {
            log.debug("embed 收到空文本，返回零向量");
            return newZeroVector();
        }
        List<List<Float>> results = batchEmbed(List.of(text));
        return results.isEmpty() ? newZeroVector() : results.get(0);
    }

    @Override
    public List<List<Float>> batchEmbed(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }

        // 过滤空文本，记录位置以便映射回原始下标
        List<String> nonEmpty = new ArrayList<>();
        List<Integer> nonEmptyIndices = new ArrayList<>();
        for (int i = 0; i < texts.size(); i++) {
            String t = texts.get(i);
            if (t != null && !t.isBlank()) {
                nonEmpty.add(t);
                nonEmptyIndices.add(i);
            }
        }

        if (nonEmpty.isEmpty()) {
            // 全部为空文本，返回等量零向量
            return createZeroVectors(texts.size());
        }

        // 分批请求
        int maxBatch = embeddingConfig.getMaxBatchSize();
        List<List<Float>> nonEmptyResults = new ArrayList<>();
        for (int i = 0; i < nonEmpty.size(); i += maxBatch) {
            int end = Math.min(i + maxBatch, nonEmpty.size());
            List<String> batch = nonEmpty.subList(i, end);
            List<List<Float>> batchResult = doEmbed(batch);
            nonEmptyResults.addAll(batchResult);
        }

        // 重建完整结果列表（含空文本的零向量）——O(n) 直接按下标回填，避免 contains/indexOf 的 O(n²)
        List<List<Float>> allResults = new ArrayList<>(Collections.nCopies(texts.size(), null));
        for (int j = 0; j < nonEmptyIndices.size(); j++) {
            allResults.set(nonEmptyIndices.get(j), nonEmptyResults.get(j));
        }
        for (int i = 0; i < allResults.size(); i++) {
            if (allResults.get(i) == null) {
                allResults.set(i, newZeroVector());
            }
        }

        return allResults;
    }

    /**
     * 执行单次 Embedding API 调用（含指数退避重试和网络异常包装）.
     * <p>
     * 与 {@link com.library.ai.llm.LlmServiceImpl#executeWithRetry} 保持一致的重试策略。
     *
     * @param texts 待向量化的文本列表（非空，1-25 条）
     * @return 向量列表
     */
    private List<List<Float>> doEmbed(List<String> texts) {
        EmbeddingRequest request = EmbeddingRequest.builder()
                .model(embeddingConfig.getEmbeddingModel())
                .input(EmbeddingRequest.Input.builder().texts(texts).build())
                .parameters(EmbeddingRequest.Parameters.builder().textType("document").build())
                .build();

        log.debug("DashScope Embedding 请求: model={}, textCount={}", embeddingConfig.getEmbeddingModel(), texts.size());

        EmbeddingResponse response = dashscopeWebClient.post()
                .uri(EMBEDDING_PATH)
                .bodyValue(request)
                .retrieve()
                .onStatus(
                        status -> status.isError(),
                        resp -> resp.bodyToMono(String.class)
                                .map(body -> new LlmUnavailableException(
                                        "DashScope Embedding API 错误: " + resp.statusCode() + " - " + body,
                                        categorizeReason(resp.statusCode().value()))))
                .bodyToMono(EmbeddingResponse.class)
                .onErrorMap(IOException.class,
                        e -> new LlmUnavailableException("DashScope Embedding 网络异常: " + e.getMessage(),
                                e, "NETWORK_ERROR"))
                .retryWhen(Retry.backoff(MAX_RETRIES, Duration.ofSeconds(1))
                        .maxBackoff(Duration.ofSeconds(8))
                        .filter(throwable -> {
                            // 跳过永久性错误重试（与 LlmServiceImpl 一致）
                            if (throwable instanceof LlmUnavailableException e) {
                                String reason = e.getReason();
                                return !"AUTH_FAILED".equals(reason)
                                        && !"QUOTA_EXHAUSTED".equals(reason)
                                        && !"CLIENT_ERROR".equals(reason);
                            }
                            return true;
                        })
                        .doBeforeRetry(signal -> log.info("DashScope Embedding 重试 ({}), 失败原因: {}",
                                signal.totalRetries() + 1, signal.failure().getMessage()))
                        .onRetryExhaustedThrow((spec, signal) ->
                                new LlmUnavailableException("DashScope Embedding 重试耗尽 (共" + MAX_RETRIES + "次)",
                                        signal.failure(), "RETRY_EXHAUSTED")))
                .block(embeddingConfig.getReadTimeout().plusSeconds(10));

        if (response == null) {
            log.warn("DashScope Embedding 返回 null，返回零向量列表");
            return createZeroVectors(texts.size());
        }

        List<List<Float>> sorted = response.sortedEmbeddings();
        // 防御：校验返回向量数与请求文本数一致
        if (sorted.size() != texts.size()) {
            log.warn("DashScope Embedding 返回向量数({})与请求文本数({})不一致，截断或补零", sorted.size(), texts.size());
            List<List<Float>> result = new ArrayList<>(texts.size());
            for (int i = 0; i < texts.size(); i++) {
                result.add(i < sorted.size() ? sorted.get(i) : newZeroVector());
            }
            return result;
        }
        log.debug("DashScope Embedding 完成: textCount={}, vectorCount={}", texts.size(), sorted.size());
        return sorted;
    }

    /**
     * 按 HTTP 状态码分类失败原因（与 LlmServiceImpl 保持一致）.
     */
    private String categorizeReason(int statusCode) {
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

    /**
     * 创建独立的零向量（避免共享 nCopies 引用导致下游 UnsupportedOperationException）.
     */
    private static List<Float> newZeroVector() {
        List<Float> vec = new ArrayList<>(EMBEDDING_DIM);
        for (int i = 0; i < EMBEDDING_DIM; i++) vec.add(0.0f);
        return vec;
    }

    /**
     * 批量创建独立零向量.
     */
    private static List<List<Float>> createZeroVectors(int count) {
        List<List<Float>> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) result.add(newZeroVector());
        return result;
    }
}
