package com.library.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.common.dto.PageDTO;
import com.library.common.exception.BizException;
import com.library.common.exception.ErrorCode;
import com.library.common.result.PageResult;
import com.library.core.entity.Book;
import com.library.core.entity.Reservation;
import com.library.core.enums.ReservationStatusEnum;
import com.library.core.mapper.BookMapper;
import com.library.core.mapper.ReservationMapper;
import com.library.core.service.BookService;
import com.library.core.service.ReservationService;
import com.library.core.vo.BookSimpleVO;
import com.library.core.vo.ReservationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 预约管理服务实现.
 * <p>
 * Redis ZSET 为主排队数据源，MySQL 为持久化记录。
 * Redis 不可用时，预约记录已落库，排队位置可能不准确但主流程不阻塞。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

    private final ReservationMapper reservationMapper;
    private final BookMapper bookMapper;
    private final BookService bookService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String QUEUE_KEY_PREFIX = "reservation:queue:";
    private static final String LOCK_KEY_PREFIX = "lock:reservation:";
    private static final long LOCK_TTL_SECONDS = 5;

    @Override
    @Transactional
    public ReservationVO reserve(Long userId, Long bookId) {
        // 1. 校验图书存在
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new BizException(ErrorCode.BOOK_NOT_FOUND);
        }

        // 2. 有库存时提示直接借阅
        if (book.getAvailCopies() > 0) {
            throw new BizException(ErrorCode.BOOK_AVAILABLE);
        }

        // 3. 获取分布式锁：防止同一用户对同一本书并发创建多条 WAITING 预约（TOCTOU 竞态）
        String lockKey = LOCK_KEY_PREFIX + userId + ":" + bookId;
        Boolean locked;
        try {
            locked = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, "1", LOCK_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Redis 预约锁获取异常: key={}, error={}", lockKey, e.getMessage());
            throw new BizException(ErrorCode.INTERNAL_ERROR);
        }
        if (!Boolean.TRUE.equals(locked)) {
            throw new BizException(ErrorCode.ALREADY_RESERVED);
        }

        try {
            // 4. 校验未重复预约（锁保护下 selectCount + insert 为原子段，配合 V6 UNIQUE 约束兜底）
            long existingCount = reservationMapper.selectCount(
                    new LambdaQueryWrapper<Reservation>()
                            .eq(Reservation::getUserId, userId)
                            .eq(Reservation::getBookId, bookId)
                            .eq(Reservation::getStatus, ReservationStatusEnum.WAITING)
            );
            if (existingCount > 0) {
                throw new BizException(ErrorCode.ALREADY_RESERVED);
            }

            // 5. 持久化预约记录（queuePosition 为近似快照：锁保护下 = 已有 WAITING 数 + 1；
            //    权威实时位置由查询接口从 Redis 计算）
            int queuePosition = (int) existingCount + 1;
            Reservation reservation = new Reservation();
            reservation.setUserId(userId);
            reservation.setBookId(bookId);
            reservation.setReserveTime(LocalDateTime.now());
            reservation.setStatus(ReservationStatusEnum.WAITING);
            reservation.setQueuePosition(queuePosition);
            reservationMapper.insert(reservation);

            log.info("预约成功: reservationId={}, userId={}, bookId={}, queuePosition={}",
                    reservation.getId(), userId, bookId, queuePosition);

            // 6. ZSET 入队 + 锁释放 延迟至事务提交后：避免 DB 回滚后 Redis 残留幽灵条目，
            //    同时覆盖"事务未提交即放锁"的提交窗口
            deferEnqueueAndLockRelease(lockKey, bookId, userId.toString());
            return toReservationVO(reservation);
        } catch (Exception e) {
            // 异常路径：事务将回滚，立即释放锁
            releaseLockSafely(lockKey);
            throw e;
        }
    }

    /**
     * 延迟到事务提交后执行 ZSET 入队并释放锁.
     * <p>
     * ZSET 入队必须在事务提交后，避免 DB 回滚后 Redis 残留幽灵条目；与锁释放共用同一
     * AFTER_COMMIT 回调，一并覆盖"事务未提交即放锁"的提交窗口。无事务上下文（单测）时立即执行。
     *
     * @param lockKey   锁键
     * @param bookId    图书 ID（ZSET key 的一部分）
     * @param userIdStr 排队成员（用户 ID 字符串）
     */
    private void deferEnqueueAndLockRelease(String lockKey, Long bookId, String userIdStr) {
        String queueKey = QUEUE_KEY_PREFIX + bookId;
        Runnable action = () -> {
            try {
                redisTemplate.opsForZSet().add(queueKey, userIdStr, System.currentTimeMillis());
            } catch (Exception e) {
                log.warn("ZSET 入队失败，预约记录已落库: bookId={}, error={}", bookId, e.getMessage());
            } finally {
                releaseLockSafely(lockKey);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            // 无事务上下文（单测场景），立即执行
            action.run();
        }
    }

    /**
     * 安全释放 Redis 锁（吞掉 Redis 异常，仅记日志）.
     *
     * @param lockKey 锁键
     */
    private void releaseLockSafely(String lockKey) {
        try {
            redisTemplate.delete(lockKey);
        } catch (Exception e) {
            log.warn("释放预约锁失败: key={}, error={}", lockKey, e.getMessage());
        }
    }

    @Override
    @Transactional
    public void cancel(Long reservationId, Long userId) {
        Reservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null) {
            throw new BizException(ErrorCode.RESERVATION_NOT_FOUND);
        }
        if (!reservation.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        if (reservation.getStatus() != ReservationStatusEnum.WAITING) {
            throw new BizException(ErrorCode.CONFLICT);
        }

        // 1. 从 Redis ZSET 移除（失败降级：DB 为权威数据源，残留条目由 ReservationNotifier
        //    的 popMin + findWaitingReservation 跳过非 WAITING 记录而安全忽略）
        String queueKey = QUEUE_KEY_PREFIX + reservation.getBookId();
        try {
            redisTemplate.opsForZSet().remove(queueKey, userId.toString());
        } catch (Exception e) {
            log.warn("Redis ZSET 移除失败，降级仅更新 DB（残留由通知器跳过）: bookId={}, error={}",
                    reservation.getBookId(), e.getMessage());
        }

        // 2. 更新 DB 状态（DB 为权威数据源）
        reservation.setStatus(ReservationStatusEnum.CANCELLED);
        reservationMapper.updateById(reservation);

        log.info("预约已取消: reservationId={}, userId={}, bookId={}",
                reservationId, userId, reservation.getBookId());
    }

    @Override
    public PageResult<ReservationVO> getMyReservations(Long userId, String status, PageDTO pageDTO) {
        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getUserId, userId)
                .orderByDesc(Reservation::getCreateTime);

        if (status != null && !status.isBlank()) {
            try {
                ReservationStatusEnum statusEnum = ReservationStatusEnum.valueOf(status.toUpperCase());
                wrapper.eq(Reservation::getStatus, statusEnum);
            } catch (IllegalArgumentException e) {
                log.debug("无效的预约状态筛选参数: {}", status);
            }
        }

        Page<Reservation> page = new Page<>(pageDTO.getPageNum(), pageDTO.getPageSize());
        IPage<Reservation> result = reservationMapper.selectPage(page, wrapper);

        // 批量转换：一次查询关联图书，消除 N+1
        List<ReservationVO> records = toReservationVOs(result.getRecords());

        return PageResult.of(records, result.getTotal(), pageDTO.getPageNum(), pageDTO.getPageSize());
    }

    @Override
    public Integer getQueuePosition(Long reservationId, Long userId) {
        Reservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null) {
            throw new BizException(ErrorCode.RESERVATION_NOT_FOUND);
        }
        // 归属校验：仅允许查询本人预约的排队位置，防横向越权
        if (!reservation.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        return queryQueuePosition(reservation.getBookId(), reservation.getUserId(),
                reservation.getQueuePosition());
    }

    // ==================== VO 转换 ====================

    /**
     * 批量转换：一次查询所有关联图书，消除分页场景的 N+1 查询.
     *
     * @param reservations 预约记录列表
     * @return 预约记录 VO 列表
     */
    private List<ReservationVO> toReservationVOs(List<Reservation> reservations) {
        if (reservations.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> bookIds = reservations.stream()
                .map(Reservation::getBookId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, BookSimpleVO> bookMap = loadBookMap(bookIds);
        // 批量预加载 WAITING 记录的排队位置（按 bookId 分组，每本书一次 zRange，消除 N+1 over Redis）
        Map<Long, Integer> positionByReservationId = batchLoadQueuePositions(reservations);
        return reservations.stream()
                .map(r -> buildReservationVO(r, bookMap.get(r.getBookId()),
                        positionByReservationId.get(r.getId())))
                .toList();
    }

    /**
     * 单条转换（预约成功返回等单条场景）.
     *
     * @param reservation 预约记录
     * @return 预约记录 VO
     */
    private ReservationVO toReservationVO(Reservation reservation) {
        Set<Long> ids = reservation.getBookId() != null
                ? Set.of(reservation.getBookId())
                : Collections.emptySet();
        return buildReservationVO(reservation, loadBookMap(ids).get(reservation.getBookId()));
    }

    /**
     * 批量加载图书并以 ID 索引；查询失败降级为空 Map，不影响主流程.
     *
     * @param bookIds 图书 ID 集合
     * @return id → BookSimpleVO 映射
     */
    private Map<Long, BookSimpleVO> loadBookMap(Set<Long> bookIds) {
        if (bookIds.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            return bookService.listByIds(new ArrayList<>(bookIds)).stream()
                    .collect(Collectors.toMap(BookSimpleVO::getId, b -> b, (a, b) -> a));
        } catch (Exception e) {
            log.warn("批量获取图书信息失败: bookIds={}, error={}", bookIds, e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * 查询排队位置；Redis 不可用时降级返回上次持久化的位置.
     *
     * @param bookId   图书 ID
     * @param userId   用户 ID
     * @param fallback 持久化的排队位置（降级用）
     * @return 实时排队位置（1-based），不在队列时返回 null
     */
    private Integer queryQueuePosition(Long bookId, Long userId, Integer fallback) {
        String queueKey = QUEUE_KEY_PREFIX + bookId;
        try {
            Long rank = redisTemplate.opsForZSet().rank(queueKey, userId.toString());
            return rank != null ? rank.intValue() + 1 : null;
        } catch (Exception e) {
            log.debug("查询排队位置失败，降级返回持久化值: bookId={}, userId={}", bookId, userId);
            return fallback;
        }
    }

    /**
     * Reservation + 关联 BookSimpleVO → ReservationVO（单条场景，实时查询排队位置）.
     */
    private ReservationVO buildReservationVO(Reservation reservation, BookSimpleVO bookVO) {
        return buildReservationVO(reservation, bookVO, null);
    }

    /**
     * Reservation + 关联 BookSimpleVO → ReservationVO.
     *
     * @param preloadedPosition 批量预加载的排队位置（null 时降级实时查询）
     */
    private ReservationVO buildReservationVO(Reservation reservation, BookSimpleVO bookVO,
                                             Integer preloadedPosition) {
        Integer queuePosition = null;
        if (reservation.getStatus() == ReservationStatusEnum.WAITING) {
            if (preloadedPosition != null) {
                queuePosition = preloadedPosition;
            } else {
                queuePosition = queryQueuePosition(
                        reservation.getBookId(), reservation.getUserId(), reservation.getQueuePosition());
            }
        }
        return ReservationVO.builder()
                .id(reservation.getId())
                .userId(reservation.getUserId())
                .book(bookVO)
                .reserveTime(reservation.getReserveTime())
                .notifyTime(reservation.getNotifyTime())
                .expireTime(reservation.getExpireTime())
                .status(reservation.getStatus() != null ? reservation.getStatus().name() : null)
                .queuePosition(queuePosition)
                .build();
    }

    /**
     * 批量查询 WAITING 预约的排队位置（按用户 rank 逐条查询）.
     * <p>
     * 改用 {@code rank} 逐条查询（O(log N)）替代原 {@code zRange 0 -1} 全量拉取：
     * 全量拉取在热门书预约队列累积到数千条时会阻塞 Redis 并占用内存；逐条 rank 仅查本页
     * 涉及用户（≤ pageSize ≤ 100），对大集合健壮。与 {@link #queryQueuePosition} 降级语义一致。
     *
     * @param reservations 预约记录列表
     * @return reservationId → 1-based 排队位置
     */
    private Map<Long, Integer> batchLoadQueuePositions(List<Reservation> reservations) {
        Map<Long, Integer> result = new HashMap<>();
        for (Reservation r : reservations) {
            if (r.getStatus() != ReservationStatusEnum.WAITING) {
                continue;
            }
            String queueKey = QUEUE_KEY_PREFIX + r.getBookId();
            try {
                Long rank = redisTemplate.opsForZSet().rank(queueKey, r.getUserId().toString());
                if (rank != null) {
                    result.put(r.getId(), rank.intValue() + 1);
                }
            } catch (Exception ex) {
                log.debug("批量查询排队位置失败，降级持久化值: reservationId={}", r.getId());
            }
        }
        return result;
    }
}
