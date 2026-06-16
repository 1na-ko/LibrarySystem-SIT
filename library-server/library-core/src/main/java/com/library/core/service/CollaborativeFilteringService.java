package com.library.core.service;

import com.library.core.entity.BorrowRecord;

import java.util.List;
import java.util.Map;

/**
 * 协同过滤推荐服务接口.
 * <p>
 * 基于借阅记录构建用户-图书交互矩阵，实现 User-CF（余弦相似度）
 * 和 Item-CF（Jaccard 相似度）两种协同过滤算法。所有计算在内存中完成。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public interface CollaborativeFilteringService {

    /**
     * 协同过滤推荐（User-CF + Item-CF 融合）.
     * <p>
     * 内部全量加载活跃借阅记录构建交互矩阵。
     *
     * @param userId 目标用户 ID
     * @return 候选图书 ID → CF 融合分数（0-1）
     */
    Map<Long, Double> recommend(Long userId);

    /**
     * 协同过滤推荐（接收预加载的全量活跃借阅记录）.
     * <p>
     * 供推荐编排层（{@code RecommendationService}）在顶层一次性加载借阅矩阵后复用，
     * 避免多路召回各自重复全表扫描 {@code borrow_record}。
     *
     * @param userId     目标用户 ID
     * @param allRecords 全量活跃借阅记录（仅含 userId、bookId）
     * @return 候选图书 ID → CF 融合分数（0-1）
     */
    Map<Long, Double> recommend(Long userId, List<BorrowRecord> allRecords);
}
