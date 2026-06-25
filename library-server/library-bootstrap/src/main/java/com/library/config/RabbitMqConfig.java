package com.library.config;

import com.library.core.event.EventBusConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 事件总线配置.
 * <p>
 * 阶段 10 引入 RabbitMQ 替代 Spring Application Events 作为模块间异步事件总线，消除架构文档
 * "Spring Events" 与技术栈表 "RabbitMQ" 的冲突。
 * <p>
 * 架构：领域事件 → {@code EventBusBridge}（{@code @TransactionalEventListener(AFTER_COMMIT)} 桥接）
 *      → {@code library.events} Topic Exchange → 业务队列 → {@code @RabbitListener} 消费。
 * <p>
 * 可靠性：消息持久化（durable queue + persistent message）+ 消费端 Spring AMQP RetryTemplate
 *      （3 次指数退避，配置见 application.yml）+ 死信队列兜底（重试耗尽投递 {@code q.library.events.dlq}）。
 * <p>
 * 双写一致性：桥接在事务提交后发 MQ，崩溃丢消息窗口极小（事务已提交、仅发 MQ 一步），
 *      ES 由 {@code EsRebuildJob} 周级全量重建兜底。Outbox 严格不丢但过重，记为未来演进。
 * <p>
 * 常量定义在 {@link EventBusConstants}（core 模块），保证依赖方向 core ← bootstrap 正确。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class RabbitMqConfig {

    // ==================== 交换器 ====================

    /**
     * 业务 Topic 交换器.
     * <p>
     * routing key 格式 {@code book.{created|updated|deleted|borrowed|returned}}。
     */
    @Bean
    public TopicExchange libraryEventsExchange() {
        return new TopicExchange(EventBusConstants.EXCHANGE, true, false);
    }

    /**
     * 死信 Topic 交换器.
     */
    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(EventBusConstants.DLX, true, false);
    }

    // ==================== 死信队列 ====================

    /**
     * 死信队列：接收所有被拒绝（重试耗尽）的消息，供人工排查.
     */
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(EventBusConstants.DLQ).build();
    }

    /**
     * 死信队列绑定到死信交换器（{@code #} 通配接收全部）.
     */
    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with("#");
    }

    // ==================== ES 同步队列 ====================

    /**
     * ES 同步队列：消费所有 {@code book.*} 事件.
     * <p>
     * 配置 {@code x-dead-letter-exchange} 使消费失败重试耗尽的消息进入死信交换器。
     */
    @Bean
    public Queue esSyncQueue() {
        return QueueBuilder.durable(EventBusConstants.QUEUE_ES_SYNC)
                .withArgument("x-dead-letter-exchange", EventBusConstants.DLX)
                .build();
    }

    /**
     * ES 同步队列绑定：{@code book.*} 匹配全部图书事件.
     */
    @Bean
    public Binding esSyncBinding() {
        return BindingBuilder.bind(esSyncQueue()).to(libraryEventsExchange()).with("book.*");
    }

    // ==================== 预约通知队列 ====================

    /**
     * 预约通知队列：仅消费 {@code book.returned}.
     */
    @Bean
    public Queue reservationNotifyQueue() {
        return QueueBuilder.durable(EventBusConstants.QUEUE_RESERVATION_NOTIFY)
                .withArgument("x-dead-letter-exchange", EventBusConstants.DLX)
                .build();
    }

    @Bean
    public Binding reservationNotifyBinding() {
        return BindingBuilder.bind(reservationNotifyQueue())
                .to(libraryEventsExchange())
                .with(EventBusConstants.RK_BOOK_RETURNED);
    }

    // ==================== KG 构建队列 ====================

    /**
     * KG 构建队列：消费 {@code book.created/updated/deleted}.
     */
    @Bean
    public Queue kgBuildQueue() {
        return QueueBuilder.durable(EventBusConstants.QUEUE_KG_BUILD)
                .withArgument("x-dead-letter-exchange", EventBusConstants.DLX)
                .build();
    }

    @Bean
    public Binding kgBuildCreatedBinding() {
        return BindingBuilder.bind(kgBuildQueue()).to(libraryEventsExchange()).with(EventBusConstants.RK_BOOK_CREATED);
    }

    @Bean
    public Binding kgBuildUpdatedBinding() {
        return BindingBuilder.bind(kgBuildQueue()).to(libraryEventsExchange()).with(EventBusConstants.RK_BOOK_UPDATED);
    }

    @Bean
    public Binding kgBuildDeletedBinding() {
        return BindingBuilder.bind(kgBuildQueue()).to(libraryEventsExchange()).with(EventBusConstants.RK_BOOK_DELETED);
    }

    // ==================== 序列化与模板 ====================

    /**
     * Jackson 消息转换器：消息体 JSON 序列化（bookId 等）.
     */
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate：发布端使用 Jackson 转换器.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
