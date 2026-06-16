package com.library.core.event;

import com.library.core.entity.Book;
import com.library.core.entity.Category;
import com.library.core.mapper.BookMapper;
import com.library.core.mapper.CategoryMapper;
import com.library.core.repository.BookDocument;
import com.library.core.repository.BookESRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Elasticsearch 数据同步监听器.
 * <p>
 * 监听领域事件，异步将 MySQL 变更同步至 Elasticsearch。
 * 遵循架构文档 §5.4：MySQL 为主存储，ES 为搜索从存储，保证最终一致性（延迟 < 1s）。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ESSyncListener {

    private final BookESRepository bookESRepository;
    private final BookMapper bookMapper;
    private final CategoryMapper categoryMapper;

    private static final int MAX_RETRIES = 3;

    /**
     * 监听图书新增事件 → ES 索引文档.
     */
    @Async
    @EventListener
    public void onBookCreated(BookCreatedEvent event) {
        syncWithRetry(event.bookId(), "新增");
    }

    /**
     * 监听图书更新事件 → ES 更新文档.
     */
    @Async
    @EventListener
    public void onBookUpdated(BookUpdatedEvent event) {
        syncWithRetry(event.bookId(), "更新");
    }

    /**
     * 监听图书删除事件 → ES 删除文档.
     */
    @Async
    @EventListener
    public void onBookDeleted(BookDeletedEvent event) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                bookESRepository.delete(event.bookId());
                log.info("ES 同步：图书删除, bookId={}", event.bookId());
                return;
            } catch (Exception e) {
                log.warn("ES 同步：图书删除失败 (attempt {}/{}), bookId={}, error={}",
                        attempt, MAX_RETRIES, event.bookId(), e.getMessage());
                if (attempt == MAX_RETRIES) {
                    log.error("ES 同步：图书删除最终失败, bookId={}", event.bookId());
                }
            }
        }
    }

    /**
     * 监听图书借出事件 → ES 更新 avail_copies 和 borrow_count.
     */
    @Async
    @EventListener
    public void onBookBorrowed(BookBorrowedEvent event) {
        syncWithRetry(event.bookId(), "借出计数更新");
    }

    /**
     * 监听图书归还事件 → ES 更新 avail_copies.
     */
    @Async
    @EventListener
    public void onBookReturned(BookReturnedEvent event) {
        syncWithRetry(event.bookId(), "归还计数更新");
    }

    /**
     * 带重试的 ES 索引/更新同步.
     */
    private void syncWithRetry(Long bookId, String operation) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                BookDocument doc = buildDocument(bookId);
                if (doc == null) {
                    log.warn("ES 同步：图书不存在或已删除, bookId={}, operation={}", bookId, operation);
                    return;
                }
                bookESRepository.save(doc);
                log.info("ES 同步：图书{}, bookId={}", operation, bookId);
                return;
            } catch (Exception e) {
                log.warn("ES 同步：图书{}失败 (attempt {}/{}), bookId={}, error={}",
                        operation, attempt, MAX_RETRIES, bookId, e.getMessage());
                if (attempt == MAX_RETRIES) {
                    log.error("ES 同步：图书{}最终失败, bookId={}", operation, bookId);
                }
            }
        }
    }

    /**
     * 从 MySQL 构建 ES 文档.
     *
     * @param bookId 图书 ID
     * @return BookDocument，图书不存在时返回 null
     */
    BookDocument buildDocument(Long bookId) {
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            return null;
        }

        String categoryName = null;
        if (book.getCategoryId() != null) {
            Category category = categoryMapper.selectById(book.getCategoryId());
            if (category != null) {
                categoryName = category.getName();
            }
        }

        // 构建 suggest 输入：书名 + 作者 + 关键词拆分
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
                .categoryName(categoryName)
                .borrowCount(book.getBorrowCount())
                .availCopies(book.getAvailCopies())
                .pubDate(book.getPubDate())
                .suggest(suggestInputs)
                .build();
    }
}
