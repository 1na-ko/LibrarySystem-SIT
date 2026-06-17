package com.library.core.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.core.config.EsIndexInitializer;
import com.library.core.entity.Book;
import com.library.core.entity.Category;
import com.library.core.mapper.BookMapper;
import com.library.core.mapper.CategoryMapper;
import com.library.core.repository.BookDocument;
import com.library.core.repository.BookESRepository;
import com.library.core.service.BookSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ES 全量索引重建 Job.
 * <p>
 * 每周日凌晨 4:00 执行：删除旧索引 → 重建 → 逐批将 MySQL 图书记录全量同步至 ES。
 * 重建完成后清除搜索缓存。
 * <p>
 * 注意：重建窗口内（通常 < 1min）搜索可能返回空或不完整结果，
 * 凌晨 4 点业务低峰期可接受。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EsRebuildJob {

    private final BookMapper bookMapper;
    private final CategoryMapper categoryMapper;
    private final BookESRepository bookESRepository;
    private final EsIndexInitializer esIndexInitializer;
    private final BookSearchService bookSearchService;

    private static final int BATCH_SIZE = 200;

    /**
     * 每周日凌晨 4:00 全量重建 ES 索引.
     */
    @Scheduled(cron = "0 0 4 * * SUN")
    public void rebuildAll() {
        log.info("ES 全量重建开始");
        long start = System.currentTimeMillis();

        try {
            esIndexInitializer.recreateIndex();
        } catch (Exception e) {
            log.error("ES 索引重建失败: {}", e.getMessage(), e);
            return;
        }

        try {
            int processed = 0;
            Long lastId = 0L;
            while (true) {
                List<Book> batch = bookMapper.selectList(
                        new LambdaQueryWrapper<Book>()
                                .gt(Book::getId, lastId)
                                .orderByAsc(Book::getId)
                                .last("LIMIT " + BATCH_SIZE));
                if (batch.isEmpty()) {
                    break;
                }

                // 批量预加载分类名 Map（消除 N+1）
                Map<Long, String> catNameMap = buildCategoryNameMap(batch);

                // 先批量构建文档，再通过 BulkRequest 一次性写入（消除逐条 save 的 N 次网络往返）
                List<BookDocument> docBatch = new ArrayList<>(batch.size());
                for (Book book : batch) {
                    try {
                        BookDocument doc = buildDocument(book, catNameMap.get(book.getCategoryId()));
                        if (doc != null) {
                            docBatch.add(doc);
                        }
                    } catch (Exception e) {
                        log.error("ES 重建构建文档失败: bookId={}, error={}", book.getId(), e.getMessage());
                    }
                }
                if (!docBatch.isEmpty()) {
                    try {
                        bookESRepository.bulkSave(docBatch);
                        processed += docBatch.size();
                    } catch (Exception e) {
                        // 批量失败（网络异常或部分项错误）→ 降级逐条写入，隔离单条失败
                        log.warn("ES 批量写入失败(本批 {} 条)，降级逐条重试: {}",
                                docBatch.size(), e.getMessage());
                        for (BookDocument doc : docBatch) {
                            try {
                                bookESRepository.save(doc);
                                processed++;
                            } catch (Exception ex) {
                                log.error("ES 重建单条降级失败: bookId={}, error={}",
                                        doc.getId(), ex.getMessage());
                            }
                        }
                    }
                }
                lastId = batch.get(batch.size() - 1).getId();
                log.debug("ES 重建进度: {} 本", processed);
            }

            bookSearchService.evictAllSearchCache();
            log.info("ES 全量重建结束: 处理 {} 本图书, 耗时 {}ms",
                    processed, System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("ES 全量重建异常: {}", e.getMessage(), e);
        }
    }

    private Map<Long, String> buildCategoryNameMap(List<Book> batch) {
        List<Long> catIds = batch.stream()
                .map(Book::getCategoryId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        if (catIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return categoryMapper.selectBatchIds(catIds).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
    }

    private BookDocument buildDocument(Book book, String categoryName) {

        List<String> suggestInputs = new java.util.ArrayList<>();
        suggestInputs.add(book.getTitle());
        suggestInputs.add(book.getAuthor());
        if (book.getKeywords() != null && !book.getKeywords().isBlank()) {
            Arrays.stream(book.getKeywords().split(","))
                    .map(String::trim)
                    .filter(k -> !k.isEmpty())
                    .forEach(suggestInputs::add);
        }

        return BookDocument.builder()
                .id(book.getId())
                .isbn(book.getIsbn())
                .title(book.getTitle())
                .author(book.getAuthor())
                .publisher(book.getPublisher())
                .description(book.getDescription())
                .keywords(book.getKeywords())
                .categoryId(book.getCategoryId())
                .categoryName(categoryName)
                .borrowCount(book.getBorrowCount())
                .availCopies(book.getAvailCopies())
                .coverUrl(book.getCoverUrl())
                .location(book.getLocation())
                .pubDate(book.getPubDate())
                .suggest(suggestInputs)
                .build();
    }
}
