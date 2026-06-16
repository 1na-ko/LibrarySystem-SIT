package com.library.core.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.common.exception.BizException;
import com.library.core.entity.Book;
import com.library.core.entity.Reservation;
import com.library.core.enums.ReservationStatusEnum;
import com.library.core.mapper.BookMapper;
import com.library.core.mapper.ReservationMapper;
import com.library.core.service.impl.ReservationServiceImpl;
import com.library.core.vo.BookSimpleVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ReservationService 单元测试.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@DisplayName("ReservationService")
@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationMapper reservationMapper;
    @Mock
    private BookMapper bookMapper;
    @Mock
    private BookService bookService;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ZSetOperations<String, Object> zSetOperations;

    @InjectMocks
    private ReservationServiceImpl reservationService;

    private Book book;

    @BeforeEach
    void setUp() {
        book = new Book();
        book.setId(10L);
        book.setTitle("深入理解Java虚拟机");
        book.setAvailCopies(0);
        book.setTotalCopies(3);
    }

    @Nested
    @DisplayName("reserve")
    class Reserve {

        @Test
        @DisplayName("库存为 0 时预约成功应返回 ReservationVO 含排队位置")
        void shouldReserveWhenStockZero() {
            when(bookMapper.selectById(10L)).thenReturn(book);
            when(reservationMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(zSetOperations.add(anyString(), anyString(), anyDouble())).thenReturn(true);
            when(zSetOperations.rank(anyString(), anyString())).thenReturn(0L);
            when(reservationMapper.insert(any(Reservation.class))).thenReturn(1);
            when(bookService.listByIds(any())).thenReturn(List.of(
                    BookSimpleVO.builder().id(10L).title("深入理解Java虚拟机").build()));

            var result = reservationService.reserve(1L, 10L);

            assertThat(result).isNotNull();
            assertThat(result.getQueuePosition()).isEqualTo(1);
            assertThat(result.getStatus()).isEqualTo("WAITING");
        }

        @Test
        @DisplayName("有库存时应抛出 BOOK_AVAILABLE 提示直接借阅")
        void shouldThrowBookAvailableWhenStockExists() {
            book.setAvailCopies(2);
            when(bookMapper.selectById(10L)).thenReturn(book);

            assertThatThrownBy(() -> reservationService.reserve(1L, 10L))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("有库存");
        }

        @Test
        @DisplayName("重复预约时应抛出 ALREADY_RESERVED")
        void shouldThrowAlreadyReservedWhenDuplicate() {
            when(bookMapper.selectById(10L)).thenReturn(book);
            when(reservationMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            assertThatThrownBy(() -> reservationService.reserve(1L, 10L))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("已预约");
        }
    }

    @Nested
    @DisplayName("cancel")
    class Cancel {

        private Reservation reservation;

        @BeforeEach
        void setUpReservation() {
            reservation = new Reservation();
            reservation.setId(1L);
            reservation.setUserId(1L);
            reservation.setBookId(10L);
            reservation.setStatus(ReservationStatusEnum.WAITING);
        }

        @Test
        @DisplayName("正常取消预约应更新状态为 CANCELLED")
        void shouldCancelSuccessfully() {
            when(reservationMapper.selectById(1L)).thenReturn(reservation);
            when(reservationMapper.updateById(any(Reservation.class))).thenReturn(1);
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(zSetOperations.remove(anyString(), anyString())).thenReturn(1L);

            reservationService.cancel(1L, 1L);

            verify(reservationMapper).updateById(any(Reservation.class));
        }

        @Test
        @DisplayName("非本人预约取消失败时应抛出 FORBIDDEN")
        void shouldThrowForbiddenWhenNotOwner() {
            when(reservationMapper.selectById(1L)).thenReturn(reservation);

            assertThatThrownBy(() -> reservationService.cancel(1L, 999L))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("权限不足");
        }

        @Test
        @DisplayName("已通知状态的预约取消应抛出 CONFLICT")
        void shouldThrowConflictWhenNotWaiting() {
            reservation.setStatus(ReservationStatusEnum.NOTIFIED);
            when(reservationMapper.selectById(1L)).thenReturn(reservation);

            assertThatThrownBy(() -> reservationService.cancel(1L, 1L))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("冲突");
        }
    }

    @Nested
    @DisplayName("getQueuePosition")
    class GetQueuePosition {

        private Reservation reservation;

        @BeforeEach
        void setUp() {
            reservation = new Reservation();
            reservation.setId(1L);
            reservation.setUserId(100L);
            reservation.setBookId(10L);
            reservation.setStatus(ReservationStatusEnum.WAITING);
            reservation.setQueuePosition(3);
        }

        @Test
        @DisplayName("本人查询排队位置应成功返回")
        void shouldReturnQueuePositionForOwner() {
            when(reservationMapper.selectById(1L)).thenReturn(reservation);
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            when(zSetOperations.rank(anyString(), eq("100"))).thenReturn(2L);

            Integer position = reservationService.getQueuePosition(1L, 100L);

            assertThat(position).isEqualTo(3); // rank=2 → 1-based position=3
        }

        @Test
        @DisplayName("非本人查询排队位置应抛出 FORBIDDEN（防横向越权）")
        void shouldThrowForbiddenWhenNotOwner() {
            when(reservationMapper.selectById(1L)).thenReturn(reservation);

            assertThatThrownBy(() -> reservationService.getQueuePosition(1L, 999L))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("权限不足");
        }

        @Test
        @DisplayName("预约记录不存在时应抛出 RESERVATION_NOT_FOUND")
        void shouldThrowNotFoundWhenRecordMissing() {
            when(reservationMapper.selectById(999L)).thenReturn(null);

            assertThatThrownBy(() -> reservationService.getQueuePosition(999L, 1L))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("不存在");
        }
    }
}
