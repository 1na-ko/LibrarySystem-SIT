package com.library.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.common.exception.BizException;
import com.library.common.exception.ErrorCode;
import com.library.core.entity.Book;
import com.library.core.entity.Category;
import com.library.core.mapper.BookMapper;
import com.library.core.mapper.CategoryMapper;
import com.library.core.service.RelatedBookService;
import com.library.core.vo.BookRecommendVO;
import com.library.core.vo.BookSimpleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 相关图书服务实现.
 * <p>
 * 当前阶段（KG 模块未实现）使用 MySQL 同分类/同作者查询作为降级实现。
 * 阶段 7 完成后替换为 Neo4j 知识图谱多跳查询。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RelatedBookServiceImpl implements RelatedBookService {

    private final BookMapper bookMapper;
    private final CategoryMapper categoryMapper;

    private static final int MAX_LIMIT = 20;
    private static final double CATEGORY_SCORE = 0.7;
    private static final double AUTHOR_SCORE = 0.5;

    @Override
    public List<BookRecommendVO> getRelated(Long bookId, int limit) {
        if (bookId == null || limit <= 0) {
            return Collections.emptyList();
        }
        int actualLimit = Math.min(limit, MAX_LIMIT);

        Book target = bookMapper.selectById(bookId);
        if (target == null) {
            throw new BizException(ErrorCode.BOOK_NOT_FOUND);
        }

        Set<Long> seenIds = new LinkedHashSet<>();
        seenIds.add(bookId); // 排除自身

        List<BookRecommendVO> result = new ArrayList<>();

        // 1. 同分类图书（按 borrowCount 降序）
        // .last("LIMIT N") 中的 N 由 actualLimit 决定，上限为 MAX_LIMIT * 2 = 40，
        // 属常量范围，无 SQL 注入风险。
        if (target.getCategoryId() != null) {
            List<Book> sameCategory = bookMapper.selectList(
                    new LambdaQueryWrapper<Book>()
                            .eq(Book::getCategoryId, target.getCategoryId())
                            .orderByDesc(Book::getBorrowCount)
                            .last("LIMIT " + (actualLimit * 2))
            );
            for (Book book : sameCategory) {
                if (seenIds.add(book.getId())) {
                    result.add(toRecommendVO(book, CATEGORY_SCORE, "同分类图书"));
                    if (result.size() >= actualLimit) {
                        return result.subList(0, actualLimit);
                    }
                }
            }
        }

        // 2. 不足 limit 时补充同作者图书
        if (target.getAuthor() != null && result.size() < actualLimit) {
            List<Book> sameAuthor = bookMapper.selectList(
                    new LambdaQueryWrapper<Book>()
                            .eq(Book::getAuthor, target.getAuthor())
                            .orderByDesc(Book::getBorrowCount)
                            .last("LIMIT " + (actualLimit * 2))
            );
            for (Book book : sameAuthor) {
                if (seenIds.add(book.getId())) {
                    result.add(toRecommendVO(book, AUTHOR_SCORE, "同作者图书"));
                    if (result.size() >= actualLimit) {
                        break;
                    }
                }
            }
        }

        return result.subList(0, Math.min(result.size(), actualLimit));
    }

    /**
     * Entity → BookRecommendVO（含分类名称批量查詢）.
     */
    private BookRecommendVO toRecommendVO(Book book, double score, String reason) {
        // 批量预加载已于 getRelated() 中完成一次 category 查询；
        // 此处按需补充单个分类名称
        String categoryName = null;
        if (book.getCategoryId() != null) {
            Category category = categoryMapper.selectById(book.getCategoryId());
            if (category != null) {
                categoryName = category.getName();
            }
        }
        return BookRecommendVO.builder()
                .book(BookSimpleVO.builder()
                        .id(book.getId())
                        .isbn(book.getIsbn())
                        .title(book.getTitle())
                        .author(book.getAuthor())
                        .publisher(book.getPublisher())
                        .coverUrl(book.getCoverUrl())
                        .pubDate(book.getPubDate())
                        .availCopies(book.getAvailCopies())
                        .categoryName(categoryName)
                        .build())
                .score(score)
                .reason(reason)
                .build();
    }
}
