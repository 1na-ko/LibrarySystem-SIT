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
import java.util.List;

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

        // 1. 先从 Redis ZSET 移除（失败则整个取消操作失败，避免残留）
        String queueKey = QUEUE_KEY_PREFIX + reservation.getBookId();
        try {
            redisTemplate.opsForZSet().remove(queueKey, userId.toString());
        } catch (Exception e) {
            log.error("Redis ZSET 移除失败，取消预约中止: bookId={}, error={}",
                    reservation.getBookId(), e.getMessage());
            throw new BizException(ErrorCode.CONFLICT);
        }

        // 2. 再更新 DB 状态（DB 为权威数据源）
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

        List<ReservationVO> records = result.getRecords().stream()
                .map(this::toReservationVO)
                .toList();

        return PageResult.of(records, result.getTotal(), pageDTO.getPageNum(), pageDTO.getPageSize());
    }

    @Override
    public Integer getQueuePosition(Long reservationId) {
        Reservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null) {
            throw new BizException(ErrorCode.RESERVATION_NOT_FOUND);
        }

        String queueKey = QUEUE_KEY_PREFIX + reservation.getBookId();
        try {
            Long rank = redisTemplate.opsForZSet().rank(queueKey, reservation.getUserId().toString());
            if (rank == null) {
                return null; // 不在队列中（可能已过期或取消）
            }
            return rank.intValue() + 1;
        } catch (Exception e) {
            log.warn("Redis ZSET 查询排队位置失败: reservationId={}, error={}",
                    reservationId, e.getMessage());
            return reservation.getQueuePosition(); // 降级返回上次记录的排队位置
        }
    }

    // ==================== VO 转换 ====================

    private ReservationVO toReservationVO(Reservation reservation) {
        BookSimpleVO bookVO = null;
        try {
            List<BookSimpleVO> books = bookService.listByIds(List.of(reservation.getBookId()));
            if (!books.isEmpty()) {
                bookVO = books.get(0);
            }
        } catch (Exception e) {
            log.warn("获取图书信息失败: bookId={}, error={}", reservation.getBookId(), e.getMessage());
        }

        // 实时获取排队位置
        Integer queuePosition = null;
        if (reservation.getStatus() == ReservationStatusEnum.WAITING) {
            String queueKey = QUEUE_KEY_PREFIX + reservation.getBookId();
            try {
                Long rank = redisTemplate.opsForZSet().rank(queueKey, reservation.getUserId().toString());
                if (rank != null) {
                    queuePosition = rank.intValue() + 1;
                }
            } catch (Exception e) {
                queuePosition = reservation.getQueuePosition();
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
}
