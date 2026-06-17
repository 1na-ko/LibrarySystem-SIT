package com.library.core.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 事件总线桥接器.
 * <p>
 * 监听 5 个领域事件，在发布者事务提交后（{@code AFTER_COMMIT}）转发至 RabbitMQ 事件总线。
 * <p>
 * 业务代码仍用 {@code ApplicationEventPublisher.publishEvent()} 发布领域事件（零改动），
 * 本桥接保留原 {@code @TransactionalEventListener(AFTER_COMMIT)} 语义——事务提交后才发 MQ，
 * 消费者读 MySQL 无脏读风险。这是阶段 10 用 RabbitMQ 替换 Spring Events 的关键：业务发布点
 * 与事务语义不变，仅把"进程内异步监听"升级为"MQ 持久化投递"。
 * <p>
 * <b>fallbackExecution 显式禁用</b>：要求所有领域事件发布点必须在 {@code @Transactional}
 * 方法内调用，无事务上下文时事件被丢弃（且 Spring 输出 WARN）。这避免了"事件已发出但
 * 数据未持久化"的脏读窗口——若上游疏忽了事务注解，事件应被显式丢弃而非偷偷送出。
 * <p>
 * 消息体为 {@code bookId}（Long），routing key 编码事件类型，消费者按
 * {@code @Header(AmqpHeaders.RECEIVED_ROUTING_KEY)} 分发。
 * <p>
 * 双写一致性：afterCommit 发 MQ，事务已提交，崩溃丢消息窗口极小；ES 由 {@code EsRebuildJob}
 * 周级全量重建兜底，KG/预约由对账 Job 兜底。Outbox 严格不丢但过重，记为未来演进。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventBusBridge {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 图书新增 → 转发至 MQ.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookCreated(BookCreatedEvent event) {
        publish(EventBusConstants.RK_BOOK_CREATED, event.bookId());
    }

    /**
     * 图书修改 → 转发至 MQ.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookUpdated(BookUpdatedEvent event) {
        publish(EventBusConstants.RK_BOOK_UPDATED, event.bookId());
    }

    /**
     * 图书删除 → 转发至 MQ.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookDeleted(BookDeletedEvent event) {
        publish(EventBusConstants.RK_BOOK_DELETED, event.bookId());
    }

    /**
     * 图书借出 → 转发至 MQ.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookBorrowed(BookBorrowedEvent event) {
        publish(EventBusConstants.RK_BOOK_BORROWED, event.bookId());
    }

    /**
     * 图书归还 → 转发至 MQ.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookReturned(BookReturnedEvent event) {
        publish(EventBusConstants.RK_BOOK_RETURNED, event.bookId());
    }

    /**
     * 转发领域事件至 RabbitMQ 事件总线.
     * <p>
     * 发送失败仅记 ERROR 日志（消息丢失）——MQ 不可用属基础设施故障，生产靠 RabbitMQ HA；
     * ES 数据由 {@code EsRebuildJob} 周级全量重建兜底。不抛异常以避免影响发布者后续流程。
     */
    private void publish(String routingKey, Long bookId) {
        try {
            rabbitTemplate.convertAndSend(EventBusConstants.EXCHANGE, routingKey, bookId);
            log.debug("事件已转发至 MQ: routingKey={}, bookId={}", routingKey, bookId);
        } catch (Exception e) {
            log.error("事件转发 MQ 失败（消息丢失，由兜底 Job 补偿）: routingKey={}, bookId={}, error={}",
                    routingKey, bookId, e.getMessage());
        }
    }
}
