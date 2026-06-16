package com.library.ai.embedding;

import java.util.List;

/**
 * 文本向量化服务接口.
 * <p>
 * 封装阿里云百炼 DashScope Embedding API (text-embedding-v3)，
 * 将文本转换为 1024 维浮点数向量。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public interface EmbeddingService {

    /**
     * 将单条文本转换为向量.
     *
     * @param text 输入文本（为空时返回 1024 维零向量）
     * @return 1024 维浮点数向量
     */
    List<Float> embed(String text);

    /**
     * 批量文本向量化.
     * <p>
     * 自动按 {@code ai.dashscope.max-batch-size}（默认 25）拆分请求，
     * 合并结果后返回与输入顺序一致的向量列表。
     *
     * @param texts 文本列表（为空时返回空列表）
     * @return 每个文本对应的 1024 维向量列表
     */
    List<List<Float>> batchEmbed(List<String> texts);
}
