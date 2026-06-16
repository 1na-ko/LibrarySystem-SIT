package com.library.ai.embedding;

import com.library.ai.config.EmbeddingConfig;
import com.library.ai.embedding.dto.EmbeddingRequest;
import com.library.ai.embedding.dto.EmbeddingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

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

    @Override
    public List<Float> embed(String text) {
        if (text == null || text.isBlank()) {
            log.debug("embed 收到空文本，返回零向量");
            return Collections.nCopies(EMBEDDING_DIM, 0.0f);
        }
        List<List<Float>> results = batchEmbed(List.of(text));
        return results.isEmpty() ? Collections.nCopies(EMBEDDING_DIM, 0.0f) : results.get(0);
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
            return Collections.nCopies(texts.size(), Collections.nCopies(EMBEDDING_DIM, 0.0f));
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

        // 重建完整结果列表（含空文本的零向量）
        List<List<Float>> allResults = new ArrayList<>(Collections.nCopies(texts.size(), null));
        List<Float> zeroVector = Collections.nCopies(EMBEDDING_DIM, 0.0f);
        for (int i = 0; i < texts.size(); i++) {
            allResults.set(i, zeroVector);
        }
        for (int i = 0; i < nonEmptyIndices.size(); i++) {
            allResults.set(nonEmptyIndices.get(i), nonEmptyResults.get(i));
        }

        return allResults;
    }

    /**
     * 执行单次 Embedding API 调用.
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
                                .map(body -> new RuntimeException("DashScope Embedding API 错误: "
                                        + resp.statusCode() + " - " + body)))
                .bodyToMono(EmbeddingResponse.class)
                .block(embeddingConfig.getReadTimeout().plusSeconds(10));

        if (response == null) {
            log.warn("DashScope Embedding 返回 null，返回零向量列表");
            return Collections.nCopies(texts.size(), Collections.nCopies(EMBEDDING_DIM, 0.0f));
        }

        List<List<Float>> sorted = response.sortedEmbeddings();
        log.debug("DashScope Embedding 完成: textCount={}, vectorCount={}", texts.size(), sorted.size());
        return sorted;
    }
}
