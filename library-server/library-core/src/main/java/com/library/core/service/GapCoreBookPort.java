package com.library.core.service;

import java.util.List;

/**
 * 学科核心书目查询端口（SPI）.
 * <p>
 * 由 {@code library-knowledge-graph} 模块提供实现（条件注入），
 * 查询 Neo4j 中某学科（Subject / Category）下 PageRank 最高的 Top-N 图书 ID。
 * 供 {@code library-acquisition} 模块的缺口分析服务消费。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public interface GapCoreBookPort {

    /**
     * 获取指定学科的核心图书 ID 列表（按 KG PageRank 降序）.
     *
     * @param subjectId 学科 ID（对应 MySQL category.id）
     * @param topN      返回数量上限
     * @return 核心图书 ID 列表（按重要性降序，无数据返回空列表）
     */
    List<Long> getCoreBookIds(Long subjectId, int topN);
}
