package com.library.core.service;

import com.library.core.vo.BookRecommendVO;

import java.util.List;

/**
 * KG 相关图书查询端口（SPI）.
 * <p>
 * 由 {@code library-knowledge-graph} 模块提供实现（条件注入），
 * {@link RelatedBookService} 适配器通过 {@code ObjectProvider} 延迟注入，
 * KG 可用时通过 Neo4j 多跳邻居获取相关图书，不可用时回退 MySQL 同分类/同作者查询。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public interface KgRelatedBookPort {

    /**
     * 通过知识图谱多跳查询获取与指定图书相关的图书.
     *
     * @param bookId 目标图书 ID
     * @param limit  返回条数上限
     * @return 相关图书推荐列表（含分数和理由）
     */
    List<BookRecommendVO> getRelated(Long bookId, int limit);
}
