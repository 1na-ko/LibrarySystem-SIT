package com.library.ai.embedding.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 阿里云百炼 DashScope Embedding 请求 DTO.
 * <p>
 * 调用 {@code /api/v1/services/embeddings/text-embedding/text-embedding} 端点。
 * 模型默认 text-embedding-v3，输出 1024 维向量。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmbeddingRequest {

    /** 模型名称，如 text-embedding-v3 */
    private String model;

    /** 输入文本 */
    private Input input;

    /** 可选参数 */
    private Parameters parameters;

    /**
     * 输入文本.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Input {
        /** 待向量化的文本列表 */
        private List<String> texts;
    }

    /**
     * 请求参数.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Parameters {
        /** 文本类型：document（文档）或 query（查询） */
        private String textType;
    }
}
