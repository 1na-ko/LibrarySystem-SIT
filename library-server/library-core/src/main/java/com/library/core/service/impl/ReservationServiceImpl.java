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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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

        // 3. 校验未重复预约（DB + Redis）
        long existingCount = reservationMapper.selectCount(
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getUserId, userId)
                        .eq(Reservation::getBookId, bookId)
                        .eq(Reservation::getStatus, ReservationStatusEnum.WAITING)
        );
        if (existingCount > 0) {
            throw new BizException(ErrorCode.ALREADY_RESERVED);
        }

        // 4. Redis ZSET 入队
        String queueKey = QUEUE_KEY_PREFIX + bookId;
        long score = System.currentTimeMillis();
        try {
            redisTemplate.opsForZSet().add(queueKey, userId.toString(), score);
        } catch (Exception e) {
            log.warn("Redis ZSET 入队失败，预约记录已落库但不准确: bookId={}, error={}", bookId, e.getMessage());
        }

        // 5. 计算排队位置
        Long rank = null;
        try {
            rank = redisTemplate.opsForZSet().rank(queueKey, userId.toString());
        } catch (Exception e) {
            log.warn("Redis ZSET 排名查询失败: bookId={}", bookId);
        }
        int queuePosition = rank != null ? rank.intValue() + 1 : 1;

        // 6. 持久化预约记录
        Reservation reservation = new Reservation();
        reservation.setUserId(userId);
        reservation.setBookId(bookId);
        reservation.setReserveTime(LocalDateTime.now());
        reservation.setStatus(ReservationStatusEnum.WAITING);
        reservation.setQueuePosition(queuePosition);
        reservationMapper.insert(reservation);

        log.info("预约成功: reservationId={}, userId={}, bookId={}, queuePosition={}",
                reservation.getId(), userId, bookId, queuePosition);

        return toReservationVO(reservation);
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
                // ignore invalid status filter
            }
        }

        Page<Reservation> page = new Page<>(pageDTO.getPageNum(), pageDTO.getPageSize());
        IPage<Reservation> result = reservationMapper.selectPage(page, wrapper);

        // 批量转换：一次查询关联图书，消除 N+1
        List<ReservationVO> records = toReservationVOs(result.getRecords());

        return PageResult.of(records, result.getTotal(), pageDTO.getPageNum(), pageDTO.getPageSize());
    }

    @Override
    public Integer getQueuePosition(Long reservationId) {
        Reservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null) {
            throw new BizException(ErrorCode.RESERVATION_NOT_FOUND);
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
        return reservations.stream()
                .map(r -> buildReservationVO(r, bookMap.get(r.getBookId())))
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
     * Reservation + 关联 BookSimpleVO → ReservationVO.
     */
    private ReservationVO buildReservationVO(Reservation reservation, BookSimpleVO bookVO) {
        Integer queuePosition = null;
        if (reservation.getStatus() == ReservationStatusEnum.WAITING) {
            queuePosition = queryQueuePosition(
                    reservation.getBookId(), reservation.getUserId(), reservation.getQueuePosition());
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
}
