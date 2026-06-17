package com.library.core.event;

/**
 * 事件总线（RabbitMQ）常量定义.
 * <p>
 * 阶段 10 引入 RabbitMQ 作为模块间异步事件总线，替代 Spring Application Events。
 * 常量集中在 core 模块（依赖方向 core ← bootstrap / kg 正确），供：
 * <ul>
 *   <li>bootstrap 的 {@code RabbitMqConfig} 声明 Exchange / Queue / Binding</li>
 *   <li>core 的 {@code EventBusBridge} 发布消息（事务提交后桥接转发）</li>
 *   <li>各 {@code @RabbitListener} 消费者引用队列名</li>
 * </ul>
 * <p>
 * 消息体统一为 {@code Long bookId}（所有领域事件均只携带 bookId，消费者查 MySQL 取最新数据），
 * routing key 编码事件类型，消费者按 {@code @Header(AmqpHeaders.RECEIVED_ROUTING_KEY)} 分发。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public final class EventBusConstants {

    private EventBusConstants() {
    }

    /** 业务 Topic 交换器 */
    public static final String EXCHANGE = "library.events";

    /** 死信 Topic 交换器（消费重试耗尽后投递） */
    public static final String DLX = "library.events.dlx";

    /** 死信队列（供人工排查） */
    public static final String DLQ = "q.library.events.dlq";

    /** ES 同步队列：消费所有 book.* 事件 */
    public static final String QUEUE_ES_SYNC = "q.es-sync";

    /** 预约通知队列：消费 book.returned */
    public static final String QUEUE_RESERVATION_NOTIFY = "q.reservation-notify";

    /** KG 构建队列：消费 book.created / book.updated / book.deleted */
    public static final String QUEUE_KG_BUILD = "q.kg-build";

    /** routing key：图书新增 */
    public static final String RK_BOOK_CREATED = "book.created";
    /** routing key：图书修改 */
    public static final String RK_BOOK_UPDATED = "book.updated";
    /** routing key：图书删除 */
    public static final String RK_BOOK_DELETED = "book.deleted";
    /** routing key：图书借出 */
    public static final String RK_BOOK_BORROWED = "book.borrowed";
    /** routing key：图书归还 */
    public static final String RK_BOOK_RETURNED = "book.returned";
}
