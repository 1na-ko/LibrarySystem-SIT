package com.library.core.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.core.entity.Reservation;
import com.library.core.enums.ReservationStatusEnum;
import com.library.core.event.BookReturnedEvent;
import com.library.core.mapper.ReservationMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ReservationExpireJob 单元测试.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@DisplayName("ReservationExpireJob")
@ExtendWith(MockitoExtension.class)
class ReservationExpireJobTest {

    @Mock
    private ReservationMapper reservationMapper;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ReservationExpireJob reservationExpireJob;

    @Test
    @DisplayName("NOTIFIED 且超时的预约应置 EXPIRED 并发布归还事件顺延下一位")
    void shouldExpireAndNotifyNextWhenOverdue() {
        Reservation reservation = new Reservation();
        reservation.setId(1L);
        reservation.setUserId(2L);
        reservation.setBookId(10L);
        reservation.setStatus(ReservationStatusEnum.NOTIFIED);
        reservation.setExpireTime(LocalDateTime.now().minusHours(1));

        // 分批扫描：首轮返回超时预约，次轮返回空以终止循环
        when(reservationMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(reservation))
                .thenReturn(List.of());
        when(reservationMapper.updateById(any(Reservation.class))).thenReturn(1);

        reservationExpireJob.expireReservations();

        verify(reservationMapper).updateById(any(Reservation.class));
        verify(eventPublisher).publishEvent(any(BookReturnedEvent.class));
    }

    @Test
    @DisplayName("无超时预约时应跳过不处理")
    void shouldSkipWhenNoExpiredReservations() {
        when(reservationMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of());

        reservationExpireJob.expireReservations();

        verify(reservationMapper, never()).updateById(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("更新行数为 0（记录已被并发变更）时不应发布归还事件")
    void shouldNotNotifyWhenUpdateReturnsZero() {
        Reservation reservation = new Reservation();
        reservation.setId(1L);
        reservation.setBookId(10L);
        reservation.setStatus(ReservationStatusEnum.NOTIFIED);
        reservation.setExpireTime(LocalDateTime.now().minusHours(1));

        when(reservationMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(reservation))
                .thenReturn(List.of());
        when(reservationMapper.updateById(any(Reservation.class))).thenReturn(0);

        reservationExpireJob.expireReservations();

        verify(eventPublisher, never()).publishEvent(any(BookReturnedEvent.class));
    }
}
