package com.library.kg.listener;

import com.library.core.event.BookCreatedEvent;
import com.library.core.event.BookDeletedEvent;
import com.library.core.event.BookUpdatedEvent;
import com.library.kg.repository.Neo4jRepository;
import com.library.kg.service.GraphBuildService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

/**
 * 知识图谱自动构建监听器.
 * <p>
 * 监听 BookCreated/Updated/Deleted 事件，异步同步 MySQL → Neo4j。
 * 使用 {@code @TransactionalEventListener(AFTER_COMMIT, fallbackExecution=true)}
 * 确保发布者事务提交后才读取 MySQL 数据。
 * Neo4j 不可用时仅记 ERROR 日志，不阻塞主流程。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class KgBuildListener {

    private final GraphBuildService graphBuildService;
    private final Neo4jRepository neo4jRepository;

    public KgBuildListener(GraphBuildService graphBuildService, Neo4jRepository neo4jRepository) {
        this.graphBuildService = graphBuildService;
        this.neo4jRepository = neo4jRepository;
    }

    private static final int MAX_RETRIES = 3;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onBookCreated(BookCreatedEvent event) {
        buildWithRetry(event.bookId(), "新增");
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onBookUpdated(BookUpdatedEvent event) {
        buildWithRetry(event.bookId(), "更新");
    }

    /**
     * 监听图书删除 → 清理 Neo4j 节点及关联关系.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onBookDeleted(BookDeletedEvent event) {
        Long bookId = event.bookId();
        log.info("KG 同步删除: bookId={}", bookId);
        try {
            String cypher = "MATCH (b:Book {id: $bookId}) DETACH DELETE b";
            neo4jRepository.execute(cypher, Map.of("bookId", bookId));
            log.info("KG 删除同步成功: bookId={}", bookId);
        } catch (Exception e) {
            log.warn("KG 删除同步失败（可能节点不存在）: bookId={}, error={}", bookId, e.getMessage());
        }
    }

    private void buildWithRetry(Long bookId, String operation) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                graphBuildService.buildGraph(bookId);
                log.info("KG 构建：图书{}成功, bookId={}", operation, bookId);
                return;
            } catch (Exception e) {
                log.warn("KG 构建：图书{}失败 (attempt {}/{}), bookId={}, error={}",
                        operation, attempt, MAX_RETRIES, bookId, e.getMessage());
                if (attempt < MAX_RETRIES) {
                    sleepWithBackoff(attempt);
                } else {
                    log.error("KG 构建：图书{}最终失败, bookId={}", operation, bookId);
                }
            }
        }
    }

    private void sleepWithBackoff(int attempt) {
        long[] backoff = {100, 500, 2000};
        try {
            Thread.sleep(backoff[attempt - 1]);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
