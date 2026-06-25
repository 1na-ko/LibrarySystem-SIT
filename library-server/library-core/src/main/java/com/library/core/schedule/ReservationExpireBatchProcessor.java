package com.library.core.schedule;

import com.library.core.entity.Reservation;
import com.library.core.enums.ReservationStatusEnum;
import com.library.core.event.BookReturnedEvent;
import com.library.core.mapper.ReservationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 预约超时批量处理器.
 * <p>
 * 从 {@link ReservationExpireJob} 中提取，确保每批在独立事务
 * （{@code REQUIRES_NEW}）中提交，避免 Spring AOP 自调用绕过事务代理。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationExpireBatchProcessor {

    private final ReservationMapper reservationMapper;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 在独立事务中处理单批超时预约，成功后发布顺延事件.
     *
     * @param batch 本批待处理的超时预约记录
     * @return 实际处理的记录数
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int processBatch(List<Reservation> batch) {
        int count = 0;
        for (Reservation reservation : batch) {
            reservation.setStatus(ReservationStatusEnum.EXPIRED);
            int rows = reservationMapper.updateById(reservation);
            if (rows > 0) {
                // 重新发布归还事件，触发 ReservationNotifier 顺延通知下一位等待者
                eventPublisher.publishEvent(new BookReturnedEvent(reservation.getBookId()));
                count++;
                log.info("预约超时已置 EXPIRED 并顺延: reservationId={}, userId={}, bookId={}",
                        reservation.getId(), reservation.getUserId(), reservation.getBookId());
            }
        }
        return count;
    }
}
