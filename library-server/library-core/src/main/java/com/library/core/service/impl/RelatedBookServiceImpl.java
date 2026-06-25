package com.library.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.common.exception.BizException;
import com.library.common.exception.ErrorCode;
import com.library.core.entity.Book;
import com.library.core.entity.Category;
import com.library.core.mapper.BookMapper;
import com.library.core.mapper.CategoryMapper;
import com.library.core.service.KgRelatedBookPort;
import com.library.core.service.RelatedBookService;
import com.library.core.vo.BookRecommendVO;
import com.library.core.vo.BookSimpleVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 相关图书服务适配器.
 * <p>
 * 优先通过 {@link KgRelatedBookPort}（KG 模块提供，条件注入）使用 Neo4j
 * 多跳邻居查询获取相关图书；KG 不可用时回退 MySQL 同分类/同作者查询（降级策略）。
 * <p>
 * 阶段 7 后 KG 路径全面生效，降级路径保证无 Neo4j 环境仍可工作。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class RelatedBookServiceImpl implements RelatedBookService {

    private final BookMapper bookMapper;
    private final CategoryMapper categoryMapper;
    private final ObjectProvider<KgRelatedBookPort> portProvider;

    public RelatedBookServiceImpl(BookMapper bookMapper,
                                  CategoryMapper categoryMapper,
                                  ObjectProvider<KgRelatedBookPort> portProvider) {
        this.bookMapper = bookMapper;
        this.categoryMapper = categoryMapper;
        this.portProvider = portProvider;
    }

    private static final int MAX_LIMIT = 20;
    private static final double CATEGORY_SCORE = 0.7;
    private static final double AUTHOR_SCORE = 0.5;

    @Override
    public List<BookRecommendVO> getRelated(Long bookId, int limit) {
        if (bookId == null || limit <= 0) {
            return Collections.emptyList();
        }
        int actualLimit = Math.min(limit, MAX_LIMIT);

        // 优先 KG 路径
        KgRelatedBookPort port = portProvider.getIfAvailable();
        if (port != null) {
            try {
                List<BookRecommendVO> kgResult = port.getRelated(bookId, actualLimit);
                if (kgResult != null && !kgResult.isEmpty()) {
                    return kgResult;
                }
            } catch (Exception e) {
                log.warn("KG 相关图书查询失败，回退 MySQL: {}", e.getMessage());
            }
        }

        Book target = bookMapper.selectById(bookId);
        if (target == null) {
            throw new BizException(ErrorCode.BOOK_NOT_FOUND);
        }

        // 批量预加载全部候选图书的分类名称——消除 N+1
        Map<Long, String> categoryNameMap = loadCategoryNameMap();

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
                    result.add(toRecommendVO(book, CATEGORY_SCORE, "同分类图书", categoryNameMap));
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
                    result.add(toRecommendVO(book, AUTHOR_SCORE, "同作者图书", categoryNameMap));
                    if (result.size() >= actualLimit) {
                        break;
                    }
                }
            }
        }

        return result.subList(0, Math.min(result.size(), actualLimit));
    }

    /**
     * 批量预加载所有分类名称（id → name），消除 N+1 查询.
     */
    private Map<Long, String> loadCategoryNameMap() {
        try {
            return categoryMapper.selectList(null).stream()
                    .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));
        } catch (Exception e) {
            log.warn("批量加载分类名称失败: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * Entity → BookRecommendVO（使用预加载的分类名称 Map 消除 N+1）.
     */
    private BookRecommendVO toRecommendVO(Book book, double score, String reason,
                                          Map<Long, String> categoryNameMap) {
        String categoryName = null;
        if (book.getCategoryId() != null) {
            categoryName = categoryNameMap.get(book.getCategoryId());
            // 降级：缓存未命中时按需查询
            if (categoryName == null) {
                Category category = categoryMapper.selectById(book.getCategoryId());
                if (category != null) {
                    categoryName = category.getName();
                }
            }
        }
        return BookRecommendVO.builder()
                .book(BookSimpleVO.from(book, categoryName))
                .score(score)
                .reason(reason)
                .build();
    }
}
