package com.library.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.common.exception.BizException;
import com.library.common.exception.ErrorCode;
import com.library.core.entity.Book;
import com.library.core.entity.Category;
import com.library.core.entity.Reservation;
import com.library.core.enums.ReservationStatusEnum;
import com.library.core.mapper.BookMapper;
import com.library.core.mapper.CategoryMapper;
import com.library.core.mapper.ReservationMapper;
import com.library.core.service.BookService;
import com.library.core.service.RelatedBookService;
import com.library.core.vo.BookDetailVO;
import com.library.core.vo.BookRecommendVO;
import com.library.core.vo.BookSimpleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 图书基础服务实现.
 * <p>
 * 仅含纯 MySQL CRUD，不含 ES 搜索逻辑（ES 搜索在 Phase 3 的 BookSearchService 中实现）。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookMapper bookMapper;
    private final CategoryMapper categoryMapper;
    private final ReservationMapper reservationMapper;
    private final RelatedBookService relatedBookService;

    @Override
    @Transactional(readOnly = true)
    public BookDetailVO getById(Long id) {
        Book book = bookMapper.selectById(id);
        if (book == null) {
            throw new BizException(ErrorCode.BOOK_NOT_FOUND);
        }
        return toDetailVO(book);
    }

    @Override
    @Transactional(readOnly = true)
    public BookDetailVO getDetail(Long id) {
        BookDetailVO vo = getById(id);

        // 补充预约人数
        long reservationCount = reservationMapper.selectCount(
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getBookId, id)
                        .eq(Reservation::getStatus, ReservationStatusEnum.WAITING)
        );
        vo.setReservationCount((int) reservationCount);

        // 补充相关图书（Service 层组装，Controller 层不再变更 VO）
        List<BookRecommendVO> related = relatedBookService.getRelated(id, 10);
        vo.setRelatedBooks(related.stream().map(BookRecommendVO::getBook).toList());

        return vo;
    }

    @Override
    @Transactional(readOnly = true)
    public BookDetailVO getByIsbn(String isbn) {
        Book book = bookMapper.selectOne(
                new LambdaQueryWrapper<Book>()
                        .eq(Book::getIsbn, isbn)
        );
        if (book == null) {
            throw new BizException(ErrorCode.BOOK_NOT_FOUND);
        }
        return toDetailVO(book);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookSimpleVO> listByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<Book> books = bookMapper.selectBatchIds(ids);

        // 批量查询分类名称，避免 N+1
        Set<Long> categoryIds = books.stream()
                .map(Book::getCategoryId)
                .filter(cid -> cid != null)
                .collect(Collectors.toSet());
        Map<Long, String> categoryNameMap = Collections.emptyMap();
        if (!categoryIds.isEmpty()) {
            categoryNameMap = categoryMapper.selectBatchIds(categoryIds).stream()
                    .collect(Collectors.toMap(Category::getId, Category::getName));
        }

        final Map<Long, String> nameMap = categoryNameMap;
        return books.stream()
                .map(book -> toSimpleVO(book, nameMap.get(book.getCategoryId())))
                .toList();
    }

    /**
     * Entity → BookDetailVO（含 categoryName 关联查询）.
     */
    BookDetailVO toDetailVO(Book book) {
        String categoryName = null;
        if (book.getCategoryId() != null) {
            Category category = categoryMapper.selectById(book.getCategoryId());
            if (category != null) {
                categoryName = category.getName();
            }
        }

        return BookDetailVO.builder()
                .id(book.getId())
                .isbn(book.getIsbn())
                .title(book.getTitle())
                .author(book.getAuthor())
                .publisher(book.getPublisher())
                .pubDate(book.getPubDate())
                .categoryId(book.getCategoryId())
                .categoryName(categoryName)
                .totalCopies(book.getTotalCopies())
                .availCopies(book.getAvailCopies())
                .description(book.getDescription())
                .coverUrl(book.getCoverUrl())
                .location(book.getLocation())
                .keywordsRaw(book.getKeywords())
                .keywordList(buildKeywordList(book.getKeywords()))
                .borrowCount(book.getBorrowCount())
                .build();
    }

    /**
     * 将逗号分隔的关键词字符串拆分为列表（trim 并过滤空串）.
     */
    static List<String> buildKeywordList(String keywords) {
        if (keywords == null || keywords.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(keywords.split(","))
                .map(String::trim)
                .filter(k -> !k.isEmpty())
                .toList();
    }

    /**
     * Entity → BookSimpleVO.
     *
     * @param book         图书实体
     * @param categoryName 分类名称（可为 null）
     */
    private BookSimpleVO toSimpleVO(Book book, String categoryName) {
        return BookSimpleVO.from(book, categoryName);
    }
}
