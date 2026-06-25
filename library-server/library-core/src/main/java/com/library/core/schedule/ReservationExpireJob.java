package com.library.core.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.core.entity.Reservation;
import com.library.core.enums.ReservationStatusEnum;
import com.library.core.event.BookReturnedEvent;
import com.library.core.mapper.ReservationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 预约超期定时检查 Job.
 * <p>
 * 每小时扫描已通知（NOTIFIED）但超过 48 小时确认窗口仍未确认的预约，置为 EXPIRED，
 * 并重新发布 {@link BookReturnedEvent}，触发 {@code ReservationNotifier} 从队列中
 * 弹出下一位等待者继续通知，从而闭合预约状态机：
 * <pre>
 *   NOTIFIED →（48h 超时未确认）→ EXPIRED → 顺延通知下一位
 * </pre>
 * <p>
 * 采用分批扫描，每批委托 {@link ReservationExpireBatchProcessor} 在独立事务中
 * （{@code REQUIRES_NEW}）提交，避免大事务长时间持锁。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationExpireJob {

    private final ReservationMapper reservationMapper;
    private final ReservationExpireBatchProcessor batchProcessor;

    static final int BATCH_SIZE = 500;

    /**
     * 每小时整点检查预约超时.
     * <p>
     * 扫描条件：status = NOTIFIED AND expire_time < 当前时间.
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void expireReservations() {
        LocalDateTime now = LocalDateTime.now();
        log.info("预约超期检查开始: time={}", now);

        int processedCount = 0;
        while (true) {
            List<Reservation> batch = reservationMapper.selectList(
                    new LambdaQueryWrapper<Reservation>()
                            .eq(Reservation::getStatus, ReservationStatusEnum.NOTIFIED)
                            .lt(Reservation::getExpireTime, now)
                            .last("LIMIT " + BATCH_SIZE)
            );
            if (batch.isEmpty()) {
                break;
            }
            processedCount += batchProcessor.processBatch(batch);
        }

        if (processedCount > 0) {
            log.info("预约超期检查结束: 处理 {} 条超时预约", processedCount);
        }
    }
}
