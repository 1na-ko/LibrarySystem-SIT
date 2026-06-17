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

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 预约 ZSET 对账 Job.
 * <p>
 * 每天凌晨 4:00 运行，交叉核对 Redis ZSET 预约队列与 DB 中的 WAITING 记录，
 * 自动修复不一致：
 * <ul>
 *   <li>幽灵条目：ZSET 中存在但 DB 不存在 → 从 ZSET 删除</li>
 *   <li>孤儿条目：DB 中存在但 ZSET 缺失 → 补回 ZSET</li>
 * </ul>
 * <p>
 * DB 为权威数据源，ZSET 不一致不影响借阅/预约主流程正确性。
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
     * 每天凌晨 4:00 执行对账（在 OverdueCheckJob 3:00 和 ReservationExpireJob 之后）.
     */
    @Scheduled(cron = "0 0 4 * * ?")
    public void reconcile() {
        log.info("预约 ZSET 对账开始");
        int ghostsRemoved = 0;
        int orphansAdded = 0;

        try {
            // 查询所有 WAITING 状态的预约
            List<Reservation> waitingReservations = reservationMapper.selectList(
                    new LambdaQueryWrapper<Reservation>()
                            .eq(Reservation::getStatus, ReservationStatusEnum.WAITING));
            Map<Long, List<Reservation>> byBook = waitingReservations.stream()
                    .collect(Collectors.groupingBy(Reservation::getBookId));

            for (Map.Entry<Long, List<Reservation>> entry : byBook.entrySet()) {
                Long bookId = entry.getKey();
                List<Reservation> dbWaiting = entry.getValue();
                String queueKey = QUEUE_KEY_PREFIX + bookId;

                Set<Long> zsetUserIds = collectZsetMembers(queueKey);
                Set<Long> dbUserIds = dbWaiting.stream()
                        .map(Reservation::getUserId)
                        .collect(Collectors.toSet());

                // 幽灵删除
                Set<Long> ghosts = new HashSet<>(zsetUserIds);
                ghosts.removeAll(dbUserIds);
                for (Long ghostUserId : ghosts) {
                    redisTemplate.opsForZSet().remove(queueKey, ghostUserId.toString());
                    ghostsRemoved++;
                    log.info("对账删除 ZSET 幽灵条目: bookId={}, userId={}", bookId, ghostUserId);
                }

                // 孤儿补回
                Set<Long> orphans = new HashSet<>(dbUserIds);
                orphans.removeAll(zsetUserIds);
                for (Reservation orphan : dbWaiting) {
                    if (orphans.contains(orphan.getUserId())) {
                        double score = orphan.getReserveTime()
                                .atZone(ZoneOffset.systemDefault())
                                .toInstant().getEpochSecond();
                        redisTemplate.opsForZSet().add(queueKey,
                                orphan.getUserId().toString(), score);
                        orphansAdded++;
                        log.info("对账补回 ZSET 孤儿条目: bookId={}, userId={}", bookId, orphan.getUserId());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("预约 ZSET 对账过程中异常: {}", e.getMessage(), e);
        }

        log.info("预约 ZSET 对账结束: 删除幽灵 {} 条, 补回孤儿 {} 条", ghostsRemoved, orphansAdded);
    }

    /**
     * 收集某个队列的所有 ZSET 成员.
     */
    private Set<Long> collectZsetMembers(String queueKey) {
        Set<Object> raw = redisTemplate.opsForZSet().range(queueKey, 0, -1);
        if (raw == null || raw.isEmpty()) {
            return Set.of();
        }
        Set<Long> result = new HashSet<>();
        for (Object obj : raw) {
            try {
                result.add(Long.parseLong(obj.toString()));
            } catch (NumberFormatException ignored) {
                // 跳过非法格式
            }
        }
        return result;
    }
}
