package com.library.core.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * {@link EventBusBridge} 单元测试.
 * <p>
 * 验证 5 个领域事件在事务提交后正确桥接转发至 RabbitMQ 对应 routing key，
 * 以及 MQ 发送失败时吞异常降级（不影响发布者，由兜底 Job 补偿）。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@DisplayName("EventBusBridge")
@ExtendWith(MockitoExtension.class)
class EventBusBridgeTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private EventBusBridge eventBusBridge;

    @Test
    @DisplayName("BookCreatedEvent 应转发至 book.created")
    void shouldForwardToBookCreatedRoutingKeyWhenBookCreatedEvent() {
        eventBusBridge.onBookCreated(new BookCreatedEvent(1L));

        verify(rabbitTemplate).convertAndSend(EventBusConstants.EXCHANGE, EventBusConstants.RK_BOOK_CREATED, 1L);
    }

    @Test
    @DisplayName("BookUpdatedEvent 应转发至 book.updated")
    void shouldForwardToBookUpdatedRoutingKeyWhenBookUpdatedEvent() {
        eventBusBridge.onBookUpdated(new BookUpdatedEvent(2L));

        verify(rabbitTemplate).convertAndSend(EventBusConstants.EXCHANGE, EventBusConstants.RK_BOOK_UPDATED, 2L);
    }

    @Test
    @DisplayName("BookDeletedEvent 应转发至 book.deleted")
    void shouldForwardToBookDeletedRoutingKeyWhenBookDeletedEvent() {
        eventBusBridge.onBookDeleted(new BookDeletedEvent(3L));

        verify(rabbitTemplate).convertAndSend(EventBusConstants.EXCHANGE, EventBusConstants.RK_BOOK_DELETED, 3L);
    }

    @Test
    @DisplayName("BookBorrowedEvent 应转发至 book.borrowed")
    void shouldForwardToBookBorrowedRoutingKeyWhenBookBorrowedEvent() {
        eventBusBridge.onBookBorrowed(new BookBorrowedEvent(4L));

        verify(rabbitTemplate).convertAndSend(EventBusConstants.EXCHANGE, EventBusConstants.RK_BOOK_BORROWED, 4L);
    }

    @Test
    @DisplayName("BookReturnedEvent 应转发至 book.returned")
    void shouldForwardToBookReturnedRoutingKeyWhenBookReturnedEvent() {
        eventBusBridge.onBookReturned(new BookReturnedEvent(5L));

        verify(rabbitTemplate).convertAndSend(EventBusConstants.EXCHANGE, EventBusConstants.RK_BOOK_RETURNED, 5L);
    }

    @Test
    @DisplayName("MQ 发送失败时应吞异常不影响发布者")
    void shouldSwallowExceptionWhenMqSendFails() {
        doThrow(new RuntimeException("MQ 不可用"))
                .when(rabbitTemplate)
                .convertAndSend(EventBusConstants.EXCHANGE, EventBusConstants.RK_BOOK_CREATED, 1L);

        // 降级：记 ERROR 日志后不抛异常，避免影响发布者后续流程（ES 由 EsRebuildJob 兜底）
        assertThatCode(() -> eventBusBridge.onBookCreated(new BookCreatedEvent(1L)))
                .doesNotThrowAnyException();

        verify(rabbitTemplate).convertAndSend(EventBusConstants.EXCHANGE, EventBusConstants.RK_BOOK_CREATED, 1L);
    }
}
