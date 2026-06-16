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
import com.library.core.vo.BookDetailVO;
import com.library.core.vo.BookSimpleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
        return books.stream()
                .map(this::toSimpleVO)
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
     */
    private BookSimpleVO toSimpleVO(Book book) {
        return BookSimpleVO.builder()
                .id(book.getId())
                .isbn(book.getIsbn())
                .title(book.getTitle())
                .author(book.getAuthor())
                .publisher(book.getPublisher())
                .coverUrl(book.getCoverUrl())
                .pubDate(book.getPubDate())
                .availCopies(book.getAvailCopies())
                .build();
    }
}
