package com.library.core.event;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.core.entity.Reservation;
import com.library.core.enums.ReservationStatusEnum;
import com.library.core.mapper.ReservationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 预约通知监听器.
 * <p>
 * 异步监听 {@link BookReturnedEvent}，从 Redis ZSET 预约队列弹出首位等待者，
 * 更新预约状态为 NOTIFIED 并设置 48 小时确认窗口。
 * <p>
 * 通知方式当前为日志输出占位，阶段 9 替换为站内信/推送。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationNotifier {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ReservationMapper reservationMapper;

    private static final String QUEUE_KEY_PREFIX = "reservation:queue:";
    private static final String EXPIRE_KEY_PREFIX = "reservation:expire:";
    private static final long CONFIRM_WINDOW_HOURS = 48;

    /**
     * 监听图书归还事件 → 检查预约队列 → 通知首位有效等待者.
     * <p>
     * 注意：Redis ZSET popMin 非事务性，若 DB 更新失败，被 pop 的条目可能丢失。
     * 生产环境应配合定期对账 Job 清理僵尸队列条目。
     */
    @Async
    @EventListener
    public void onBookReturned(BookReturnedEvent event) {
        String queueKey = QUEUE_KEY_PREFIX + event.bookId();

        try {
            // 循环 pop 直到找到有效的 WAITING 预约记录（跳过已取消/已过期的残留条目）
            int attempts = 0;
            int maxAttempts = 50; // 安全上限，避免无限循环

            while (attempts < maxAttempts) {
                attempts++;

                Set<ZSetOperations.TypedTuple<Object>> popped =
                        redisTemplate.opsForZSet().popMin(queueKey, 1);
                if (popped == null || popped.isEmpty()) {
                    log.debug("预约队列已空: bookId={}", event.bookId());
                    return;
                }

                ZSetOperations.TypedTuple<Object> tuple = popped.iterator().next();
                if (tuple.getValue() == null) {
                    continue;
                }

                String userIdStr = String.valueOf(tuple.getValue());
                long userId;
                try {
                    userId = Long.parseLong(userIdStr);
                } catch (NumberFormatException e) {
                    log.warn("Redis ZSET 中的 userId 格式异常，跳过: {}", userIdStr);
                    continue;
                }

                // 查询该用户对该书的 WAITING 预约记录
                Reservation reservation = findWaitingReservation(userId, event.bookId());
                if (reservation == null) {
                    log.warn("ZSET 中有残留条目但 DB 中无 WAITING 记录（可能已取消），跳过: userId={}, bookId={}",
                            userId, event.bookId());
                    continue;
                }

                // 更新预约状态为 NOTIFIED
                LocalDateTime now = LocalDateTime.now();
                reservation.setStatus(ReservationStatusEnum.NOTIFIED);
                reservation.setNotifyTime(now);
                reservation.setExpireTime(now.plusHours(CONFIRM_WINDOW_HOURS));
                int rows = reservationMapper.updateById(reservation);
                if (rows == 0) {
                    log.error("预约状态更新失败（乐观锁冲突或记录已变更）: reservationId={}, userId={}",
                            reservation.getId(), userId);
                    // 已从 ZSET pop 但 DB 未更新 → 数据不一致，记录日志供人工处理
                    return;
                }

                log.info("预约通知已发送: reservationId={}, userId={}, bookId={}, expireTime={}",
                        reservation.getId(), userId, event.bookId(), reservation.getExpireTime());

                // 设置 48h 确认窗口过期标记
                String expireKey = EXPIRE_KEY_PREFIX + reservation.getId();
                try {
                    redisTemplate.opsForValue().set(expireKey, "1", CONFIRM_WINDOW_HOURS, TimeUnit.HOURS);
                } catch (Exception e) {
                    log.warn("设置预约过期标记失败: reservationId={}, error={}",
                            reservation.getId(), e.getMessage());
                }

                return; // 成功通知，退出循环
            }

            log.warn("预约通知达到最大尝试次数: bookId={}, maxAttempts={}", event.bookId(), maxAttempts);

        } catch (Exception e) {
            log.error("预约通知处理失败: bookId={}, error={}", event.bookId(), e.getMessage());
        }
    }

    /**
     * 查找用户对指定图书的最新 WAITING 预约记录.
     */
    private Reservation findWaitingReservation(Long userId, Long bookId) {
        return reservationMapper.selectOne(
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getUserId, userId)
                        .eq(Reservation::getBookId, bookId)
                        .eq(Reservation::getStatus, ReservationStatusEnum.WAITING)
                        .orderByAsc(Reservation::getReserveTime)
                        .last("LIMIT 1")
        );
    }
}
