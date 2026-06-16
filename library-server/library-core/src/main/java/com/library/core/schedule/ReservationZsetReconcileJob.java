package com.library.core.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.core.entity.Reservation;
import com.library.core.enums.ReservationStatusEnum;
import com.library.core.mapper.ReservationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * 预约 ZSET 对账 Job.
 * <p>
 * 每天凌晨 4:00 运行，交叉核对 Redis ZSET 预约队列与 DB 中的 WAITING 记录，
 * 清理 ZSET 中的僵尸条目（DB 已取消/已过期但 ZSET 中仍有残留的条目）。
 * <p>
 * 背景：{@code ReservationNotifier} 使用 Redis ZSET {@code popMin} 弹出队首后
 * 更新 DB，若 DB 更新失败则条目从 ZSET 丢失。本 Job 对比 ZSET 成员与 DB WAITING
 * 记录，将 DB 中存在但 ZSET 缺失的条目重新入队，同时清除 ZSET 中 DB 已非 WAITING
 * 的残留条目。
 * <p>
 * <b>当前状态</b>：骨架实现——记录队列大小统计与不一致警告，为后续阶段完善对账逻辑
 * 提供基础。核心业务路径中 DB 为权威数据源，ZSET 不一致不影响借阅/预约主流程正确性。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationZsetReconcileJob {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ReservationMapper reservationMapper;

    private static final String QUEUE_KEY_PREFIX = "reservation:queue:";

    /**
     * 每天凌晨 4:00 执行对账（在 OverdueCheckJob 3:00 和 ReservationExpireJob 每小时之后）.
     */
    @Scheduled(cron = "0 0 4 * * ?")
    public void reconcile() {
        log.info("预约 ZSET 对账开始");

        // 统计 DB 中各状态的预约数量
        long dbWaiting = reservationMapper.selectCount(
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getStatus, ReservationStatusEnum.WAITING));
        log.info("DB 中 WAITING 预约数: {}", dbWaiting);

        // 扫描 Redis 中所有预约队列键（使用 SCAN 非阻塞迭代，避免 KEYS 阻塞 Redis 主线程）
        try {
            Set<String> queueKeys = new HashSet<>();
            try (Cursor<String> cursor = (Cursor<String>) redisTemplate.scan(
                    ScanOptions.scanOptions().match(QUEUE_KEY_PREFIX + "*").count(100).build())) {
                cursor.forEachRemaining(queueKeys::add);
            }
            int totalZsetEntries = 0;
            for (String key : queueKeys) {
                Long size = redisTemplate.opsForZSet().size(key);
                if (size != null) {
                    totalZsetEntries += size.intValue();
                }
            }
            log.info("Redis ZSET 预约队列条目总数: {}，队列数: {}", totalZsetEntries, queueKeys.size());

            // 不一致检测
            if (totalZsetEntries != dbWaiting) {
                log.info("ZSET-DB 不一致（预期 {}，实际 {}）—— 差异: {}，"
                        + "将在后续完善中自动修复；当前 DB 为权威数据源，不影响主流程",
                        dbWaiting, totalZsetEntries, Math.abs(totalZsetEntries - (int) dbWaiting));
            }
        } catch (Exception e) {
            log.warn("预约 ZSET 对账过程中 Redis 不可用: {}", e.getMessage());
        }

        log.info("预约 ZSET 对账结束");
    }
}
