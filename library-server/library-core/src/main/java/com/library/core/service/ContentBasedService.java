package com.library.core.service;

import java.util.Map;
import java.util.Set;

/**
 * 基于内容的推荐服务接口.
 * <p>
 * 使用 Embedding 向量化图书文本特征（标题 + 关键词），构建用户画像向量
 * （已借图书向量的逐元素均值），通过余弦相似度匹配候选图书。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public interface ContentBasedService {

    /**
     * 基于用户借阅历史构建内容画像，推荐相似图书.
     * <p>
     * EmbeddingService 未就绪时静默返回空 Map。
     *
     * @param userId 目标用户 ID
     * @param topN   返回候选数上限
     * @return 候选图书 ID → 内容相似度分数（0-1）
     */
    Map<Long, Double> recommend(Long userId, int topN);

    /**
     * 基于内容推荐（接收预加载的已借阅图书 ID 集合）.
     * <p>
     * 供推荐编排层（{@code RecommendationService}）在顶层一次性加载借阅记录后复用，
     * 避免多路召回各自重复全表扫描 {@code borrow_record}。
     *
     * @param userId          目标用户 ID
     * @param borrowedBookIds 用户已借阅图书 ID 集合
     * @param topN            返回候选数上限
     * @return 候选图书 ID → 内容相似度分数（0-1）
     */
    Map<Long, Double> recommend(Long userId, Set<Long> borrowedBookIds, int topN);
}
