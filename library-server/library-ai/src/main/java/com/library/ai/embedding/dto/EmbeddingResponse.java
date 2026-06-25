package com.library.ai.embedding.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 阿里云百炼 DashScope Embedding 响应 DTO.
 * <p>
 * 通过 {@link #sortedEmbeddings()} 获取按 text_index 排序的向量列表，
 * 保证与请求文本顺序一致。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingResponse {

    /** 输出结果 */
    private Output output;

    /** Token 用量 */
    private Usage usage;

    /** 请求唯一标识 */
    @JsonProperty("request_id")
    private String requestId;

    /**
     * 输出结果.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Output {
        /** 向量列表 */
        private List<EmbeddingItem> embeddings;
    }

    /**
     * 单条文本的向量结果.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmbeddingItem {
        /** 对应请求中 texts 数组的下标 */
        @JsonProperty("text_index")
        private int textIndex;

        /** 向量（1024 维浮点数） */
        private List<Float> embedding;
    }

    /**
     * Token 用量.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Usage {
        /** 总 token 数 */
        @JsonProperty("total_tokens")
        private int totalTokens;
    }

    /**
     * 按 text_index 排序后返回向量列表，保证与请求文本顺序一致.
     *
     * @return 按原始文本顺序排列的向量列表
     */
    public List<List<Float>> sortedEmbeddings() {
        if (output == null || output.getEmbeddings() == null || output.getEmbeddings().isEmpty()) {
            return List.of();
        }
        List<EmbeddingItem> items = new ArrayList<>(output.getEmbeddings());
        items.sort(Comparator.comparingInt(EmbeddingItem::getTextIndex));
        return items.stream()
                .map(EmbeddingItem::getEmbedding)
                .toList();
    }
}
