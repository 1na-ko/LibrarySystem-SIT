package com.library.core.service;

import java.util.Map;

/**
 * 知识图谱推荐服务接口.
 * <p>
 * 通过 Neo4j 多跳查询获取知识层面的关联图书。
 * 阶段 6 使用桩实现（返回空），阶段 7 完成后替换为真实 Neo4j 查询。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public interface KGBasedRecommendService {

    /**
     * 基于知识图谱获取与用户借阅历史相关的图书.
     *
     * @param userId 目标用户 ID
     * @param topN   返回候选数
     * @return 候选图书 ID → KG 分数（0-1），KG 未就绪时返回空 Map
     */
    Map<Long, Double> recommend(Long userId, int topN);
}
