package com.library.core.service;

import com.library.core.vo.BookRecommendVO;

import java.util.List;

/**
 * 相关图书服务接口.
 * <p>
 * 当前阶段（KG 模块未实现）使用 MySQL 查询（同分类/同作者）作为降级实现，
 * 阶段 7 完成后替换为 Neo4j 知识图谱多跳查询。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public interface RelatedBookService {

    /**
     * 获取与指定图书相关的图书.
     * <p>
     * 策略：优先同分类图书（按 borrowCount 降序），不足 limit 时补充同作者图书。
     *
     * @param bookId 目标图书 ID
     * @param limit  返回条数上限（最大 20）
     * @return 相关图书推荐列表（含分数和理由）
     */
    List<BookRecommendVO> getRelated(Long bookId, int limit);
}
