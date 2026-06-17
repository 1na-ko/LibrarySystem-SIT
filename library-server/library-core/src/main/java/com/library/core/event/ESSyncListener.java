package com.library.core.event;

import com.library.core.entity.Book;
import com.library.core.entity.Category;
import com.library.core.mapper.BookMapper;
import com.library.core.mapper.CategoryMapper;
import com.library.core.repository.BookDocument;
import com.library.core.repository.BookESRepository;
import com.library.core.service.BookSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Elasticsearch 数据同步消费者.
 * <p>
 * 阶段 10 改为 {@code @RabbitListener} 消费 {@code q.es-sync} 队列（原
 * {@code @TransactionalEventListener(AFTER_COMMIT)} 的提交后语义由 {@link EventBusBridge} 保留）。
 * 监听领域事件，将 MySQL 变更同步至 Elasticsearch，遵循架构文档 §5.4：MySQL 为主存储，
 * ES 为搜索从存储，保证最终一致性（延迟 < 1s）。
 * <p>
 * 按 routing key 分发：{@code book.deleted} → 删除 ES 文档；其余（created/updated/borrowed/returned）
 * → 重建文档。消费失败由 Spring AMQP RetryTemplate（3 次指数退避）重试，耗尽进死信队列。
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
    private final BookSearchService bookSearchService;

    /**
     * 消费 MQ 图书事件 → 同步 ES.
     * <p>
     * 缓存失效策略：仅 created/updated/deleted 影响搜索结果集（标题/作者/分类等元数据变更），
     * 借/还（borrowed/returned）仅改变 availCopies/borrowCount，对全文检索结果集无显著影响，
     * 不参与全量搜索缓存失效，避免高频借还导致的缓存命中率塌陷。
     * <p>
     * 时序保证：先成功写入 ES，再清除搜索缓存。若先 evict 后 ES 写失败，重试期间会出现
     * "缓存空 + ES 旧数据"窗口，所有搜索请求穿透到 ES 形成击穿。
     *
     * @param bookId     图书 ID（消息体）
     * @param routingKey 事件类型（book.created/updated/deleted/borrowed/returned）
     */
    @RabbitListener(queues = EventBusConstants.QUEUE_ES_SYNC)
    public void onBookEvent(Long bookId,
                            @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) {
        if (EventBusConstants.RK_BOOK_DELETED.equals(routingKey)) {
            deleteFromEs(bookId);
            evictSearchCacheIfStructureChanged(routingKey);
            return;
        }
        syncToEs(bookId, routingKey);
        evictSearchCacheIfStructureChanged(routingKey);
    }

    /**
     * 仅在搜索结果结构性变更时清除搜索缓存（created/updated/deleted）.
     * <p>
     * borrowed/returned 不触发：避免高频借还操作打掉搜索缓存。
     */
    private void evictSearchCacheIfStructureChanged(String routingKey) {
        if (EventBusConstants.RK_BOOK_CREATED.equals(routingKey)
                || EventBusConstants.RK_BOOK_UPDATED.equals(routingKey)
                || EventBusConstants.RK_BOOK_DELETED.equals(routingKey)) {
            bookSearchService.evictAllSearchCache();
        }
    }

    /**
     * 重建 ES 文档（created/updated/borrowed/returned 共用）.
     * <p>
     * 图书不存在（已逻辑删除）时跳过，不视为异常以免触发重试。
     */
    private void syncToEs(Long bookId, String routingKey) {
        BookDocument doc = buildDocument(bookId);
        if (doc == null) {
            log.warn("ES 同步：图书不存在或已删除, bookId={}, routingKey={}", bookId, routingKey);
            return;
        }
        bookESRepository.save(doc);
        log.info("ES 同步：bookId={}, routingKey={}", bookId, routingKey);
    }

    /**
     * 从 ES 删除文档.
     */
    private void deleteFromEs(Long bookId) {
        bookESRepository.delete(bookId);
        log.info("ES 同步：图书删除, bookId={}", bookId);
    }

    /**
     * 从 MySQL 构建 ES 文档.
     *
     * @param bookId 图书 ID
     * @return BookDocument，图书不存在时返回 null
     */
    public BookDocument buildDocument(Long bookId) {
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
