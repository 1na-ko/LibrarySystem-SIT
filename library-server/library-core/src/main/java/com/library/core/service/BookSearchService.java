package com.library.core.service;

import com.library.common.result.PageResult;
import com.library.core.dto.BookAdvancedSearchDTO;
import com.library.core.dto.BookSearchDTO;
import com.library.core.vo.BookSimpleVO;
import com.library.core.vo.SuggestVO;

import java.util.List;

/**
 * 图书搜索服务接口.
 * <p>
 * 整合 Redis 缓存 + Elasticsearch 搜索，提供全文搜索、高级搜索、自动补全和热门图书功能。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public interface BookSearchService {

    /**
     * 关键词全文搜索.
     * <p>
     * Redis 缓存热点词（TTL 30min），命中直接返回，未命中穿透查 ES 并回写缓存。
     *
     * @param dto 搜索请求参数
     * @return 分页搜索结果
     */
    PageResult<BookSimpleVO> search(BookSearchDTO dto);

    /**
     * 高级组合搜索.
     *
     * @param dto 高级搜索请求参数
     * @return 分页搜索结果
     */
    PageResult<BookSimpleVO> advancedSearch(BookAdvancedSearchDTO dto);

    /**
     * 搜索自动补全.
     *
     * @param prefix 输入前缀
     * @param limit  返回条数上限
     * @return 补全建议列表
     */
    List<SuggestVO> suggest(String prefix, int limit);

    /**
     * 热门图书榜.
     *
     * @param categoryId 分类筛选（可选）
     * @param limit      返回条数
     * @return 热门图书列表
     */
    List<BookSimpleVO> hotBooks(Long categoryId, int limit);
}
