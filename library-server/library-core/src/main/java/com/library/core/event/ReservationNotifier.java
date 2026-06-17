package com.library.core.event;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.core.entity.Reservation;
import com.library.core.enums.ReservationStatusEnum;
import com.library.core.mapper.ReservationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 预约通知消费者.
 * <p>
 * 阶段 10 改为 {@code @RabbitListener} 消费 {@code q.reservation-notify} 队列（原
 * {@code @TransactionalEventListener(AFTER_COMMIT)} 的提交后语义由 {@link EventBusBridge} 保留）。
 * <p>
 * 消费图书归还事件，从 Redis ZSET 预约队列弹出首位等待者，更新预约状态为 NOTIFIED 并设置
 * 48 小时确认窗口。通知方式当前为日志输出占位，阶段 9 替换为站内信/推送。
 * <p>
 * 注意：Redis ZSET popMin 非事务性，若 DB 更新失败，被 pop 的条目可能丢失。生产环境应配合
 * 定期对账 Job（{@code ReservationZsetReconcileJob}）清理僵尸队列条目。本消费者内部 catch
 * 异常不抛（容错，避免阻塞 MQ 消费），失败由对账 Job 兜底。
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
     * 消费图书归还事件 → 检查预约队列 → 通知首位有效等待者.
     * <p>
     * 异常处理策略：
     * <ul>
     *   <li>业务级异常（NumberFormatException、单条记录格式错误）—— catch 跳过该条继续，不重试</li>
     *   <li>基础设施级异常（Redis/DB 临时不可达）—— 不 catch，由 Spring AMQP RetryTemplate
     *       重试 3 次（指数退避），耗尽后进入死信队列 q.library.events.dlq 由对账 Job 兜底</li>
     * </ul>
     * 之前版本全局 catch Exception 吞噬所有异常，导致『3 次重试 + DLQ 兜底』架构承诺失效。
     *
     * @param bookId 归还的图书 ID（消息体）
     */
    @RabbitListener(queues = EventBusConstants.QUEUE_RESERVATION_NOTIFY)
    public void onBookReturned(Long bookId) {
        String queueKey = QUEUE_KEY_PREFIX + bookId;

        // 循环 pop 直到找到有效的 WAITING 预约记录（跳过已取消/已过期的残留条目）
        int attempts = 0;
        int maxAttempts = 50; // 安全上限，避免无限循环

        while (attempts < maxAttempts) {
            attempts++;

            // 注：Redis/DB 临时异常此处不 catch，自然抛出由 Spring AMQP RetryTemplate 接管
            Set<ZSetOperations.TypedTuple<Object>> popped =
                    redisTemplate.opsForZSet().popMin(queueKey, 1);
            if (popped == null || popped.isEmpty()) {
                log.debug("预约队列已空: bookId={}", bookId);
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
                // 业务级异常：单条数据格式错误 → 跳过继续，不触发重试
                log.warn("Redis ZSET 中的 userId 格式异常，跳过: {}", userIdStr);
                continue;
            }

            // 查询该用户对该书的 WAITING 预约记录
            Reservation reservation = findWaitingReservation(userId, bookId);
            if (reservation == null) {
                log.warn("ZSET 中有残留条目但 DB 中无 WAITING 记录（可能已取消），跳过: userId={}, bookId={}",
                        userId, bookId);
                continue;
            }

            // 更新预约状态为 NOTIFIED
            LocalDateTime now = LocalDateTime.now();
            reservation.setStatus(ReservationStatusEnum.NOTIFIED);
            reservation.setNotifyTime(now);
            reservation.setExpireTime(now.plusHours(CONFIRM_WINDOW_HOURS));
            int rows = reservationMapper.updateById(reservation);
            if (rows == 0) {
                // Reservation 实体未启用 @Version 乐观锁，updateById 按主键更新；
                // rows==0 意味着记录在 select 后被并发删除或逻辑删除，需人工核对
                log.error("预约状态更新失败（记录在 select 后被变更/删除）: reservationId={}, userId={}",
                        reservation.getId(), userId);
                return;
            }

            log.info("预约通知已发送: reservationId={}, userId={}, bookId={}, expireTime={}",
                    reservation.getId(), userId, bookId, reservation.getExpireTime());

            // 设置 48h 确认窗口过期标记（失败仅日志，不阻塞主通知流程）
            String expireKey = EXPIRE_KEY_PREFIX + reservation.getId();
            try {
                redisTemplate.opsForValue().set(expireKey, "1", CONFIRM_WINDOW_HOURS, TimeUnit.HOURS);
            } catch (Exception e) {
                log.warn("设置预约过期标记失败: reservationId={}, error={}",
                        reservation.getId(), e.getMessage());
            }

            return; // 成功通知，退出循环
        }

        log.warn("预约通知达到最大尝试次数: bookId={}, maxAttempts={}", bookId, maxAttempts);
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
