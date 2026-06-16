package com.library.core.service;

import com.library.core.vo.BookDetailVO;
import com.library.core.vo.BookSimpleVO;

import java.util.List;

/**
 * 图书基础服务接口.
 * <p>
 * 仅含纯 MySQL 查询，不含 ES 搜索（ES 搜索将在 Phase 3 的 BookSearchService 中实现）。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public interface BookService {

    /**
     * 根据 ID 获取图书详情.
     *
     * @param id 图书 ID
     * @return 图书详情 VO（含 categoryName）
     * @throws com.library.common.exception.BizException 图书不存在时抛出 BOOK_NOT_FOUND
     */
    BookDetailVO getById(Long id);

    /**
     * 根据 ISBN 精确查询图书.
     *
     * @param isbn ISBN 号
     * @return 图书详情 VO，不存在时返回 null
     */
    BookDetailVO getByIsbn(String isbn);

    /**
     * 批量查询图书（用于借阅/预约/推荐场景嵌入）.
     *
     * @param ids 图书 ID 列表
     * @return 图书精简视图列表
     */
    List<BookSimpleVO> listByIds(List<Long> ids);
}
