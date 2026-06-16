package com.library.core.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.core.entity.Reservation;
import com.library.core.enums.ReservationStatusEnum;
import com.library.core.mapper.ReservationMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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
    private ReservationExpireBatchProcessor batchProcessor;

    @InjectMocks
    private ReservationExpireJob reservationExpireJob;

    @Test
    @DisplayName("NOTIFIED 且超时的预约应委托 batchProcessor 处理")
    void shouldDelegateToBatchProcessorWhenOverdue() {
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
        when(batchProcessor.processBatch(anyList())).thenReturn(1);

        reservationExpireJob.expireReservations();

        verify(batchProcessor).processBatch(anyList());
    }

    @Test
    @DisplayName("无超时预约时应跳过不处理")
    void shouldSkipWhenNoExpiredReservations() {
        when(reservationMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of());

        reservationExpireJob.expireReservations();

        verify(batchProcessor, never()).processBatch(anyList());
    }
}
