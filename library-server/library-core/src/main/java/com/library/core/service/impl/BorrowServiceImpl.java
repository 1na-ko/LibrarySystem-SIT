package com.library.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.common.dto.PageDTO;
import com.library.common.exception.BizException;
import com.library.common.exception.ErrorCode;
import com.library.common.result.PageResult;
import com.library.core.entity.Book;
import com.library.core.entity.BorrowRecord;
import com.library.core.entity.FineRecord;
import com.library.core.entity.Reservation;
import com.library.core.entity.SysUser;
import com.library.core.enums.BorrowStatusEnum;
import com.library.core.enums.ReservationStatusEnum;
import com.library.core.enums.UserStatusEnum;
import com.library.core.event.BookBorrowedEvent;
import com.library.core.event.BookReturnedEvent;
import com.library.core.mapper.BookMapper;
import com.library.core.mapper.BorrowRecordMapper;
import com.library.core.mapper.FineRecordMapper;
import com.library.core.mapper.ReservationMapper;
import com.library.core.mapper.SysUserMapper;
import com.library.core.service.BorrowService;
import com.library.core.service.BookService;
import com.library.core.vo.BookSimpleVO;
import com.library.core.vo.BorrowRecordVO;
import com.library.core.vo.BorrowResultVO;
import com.library.core.vo.RenewResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 借阅管理服务实现.
 * <p>
 * 借书使用 Redis 分布式锁（SETNX + TTL 5s）+ MyBatis-Plus 乐观锁双重防护，
 * 确保高并发下库存不超卖。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BorrowServiceImpl implements BorrowService {

    private final SysUserMapper sysUserMapper;
    private final BookMapper bookMapper;
    private final BorrowRecordMapper borrowRecordMapper;
    private final ReservationMapper reservationMapper;
    private final FineRecordMapper fineRecordMapper;
    private final BookService bookService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ApplicationEventPublisher eventPublisher;

    private static final int DEFAULT_LOAN_DAYS = 30;
    private static final int RENEW_EXTEND_DAYS = 30;
    private static final BigDecimal DAILY_FINE = new BigDecimal("0.50");
    private static final String LOCK_KEY_PREFIX = "lock:borrow:";
    private static final long LOCK_TTL_SECONDS = 5;

    // ==================== 借书 ====================

    @Override
    @Transactional
    public BorrowResultVO borrow(Long userId, Long bookId) {
        // 1. 校验用户状态
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        if (user.getStatus() != UserStatusEnum.ACTIVE) {
            throw new BizException(ErrorCode.ACCOUNT_FROZEN);
        }

        // 2. 校验图书存在且有库存
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new BizException(ErrorCode.BOOK_NOT_FOUND);
        }
        if (book.getAvailCopies() <= 0) {
            throw new BizException(ErrorCode.BOOK_STOCK_EMPTY);
        }

        // 3. 校验借阅数量未超上限
        long activeBorrowCount = borrowRecordMapper.selectCount(
                new LambdaQueryWrapper<BorrowRecord>()
                        .eq(BorrowRecord::getUserId, userId)
                        .in(BorrowRecord::getStatus, BorrowStatusEnum.BORROWED, BorrowStatusEnum.RENEWED)
        );
        if (activeBorrowCount >= user.getMaxBooks()) {
            throw new BizException(ErrorCode.BORROW_LIMIT_EXCEEDED, user.getMaxBooks());
        }

        // 4. 校验未重复借阅
        long duplicateCount = borrowRecordMapper.selectCount(
                new LambdaQueryWrapper<BorrowRecord>()
                        .eq(BorrowRecord::getUserId, userId)
                        .eq(BorrowRecord::getBookId, bookId)
                        .in(BorrowRecord::getStatus, BorrowStatusEnum.BORROWED, BorrowStatusEnum.RENEWED)
        );
        if (duplicateCount > 0) {
            throw new BizException(ErrorCode.ALREADY_BORROWED);
        }

        // 5. 校验无超期未还
        long overdueCount = borrowRecordMapper.selectCount(
                new LambdaQueryWrapper<BorrowRecord>()
                        .eq(BorrowRecord::getUserId, userId)
                        .eq(BorrowRecord::getStatus, BorrowStatusEnum.OVERDUE)
        );
        if (overdueCount > 0) {
            throw new BizException(ErrorCode.OVERDUE_UNRETURNED);
        }

        // 6. Redis 分布式锁（防并发超卖）
        String lockKey = LOCK_KEY_PREFIX + bookId;
        Boolean locked;
        try {
            locked = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, "1", LOCK_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Redis 分布式锁获取异常: key={}, error={}", lockKey, e.getMessage());
            throw new BizException(ErrorCode.CONFLICT);
        }
        if (!Boolean.TRUE.equals(locked)) {
            // locked == false（锁已被持有）或 null（Redis 异常）→ 拒绝
            throw new BizException(ErrorCode.CONFLICT);
        }

        try {
            // 刷新实体以获取最新 version（乐观锁需要）
            book = bookMapper.selectById(bookId);
            if (book.getAvailCopies() <= 0) {
                throw new BizException(ErrorCode.BOOK_STOCK_EMPTY);
            }

            // 7. 扣减库存（乐观锁 version 自动校验）
            book.setAvailCopies(book.getAvailCopies() - 1);
            book.setBorrowCount(book.getBorrowCount() + 1);
            int rows = bookMapper.updateById(book);
            if (rows == 0) {
                // 乐观锁冲突
                throw new BizException(ErrorCode.CONFLICT);
            }

            // 8. 创建借阅记录
            LocalDate today = LocalDate.now();
            BorrowRecord record = new BorrowRecord();
            record.setUserId(userId);
            record.setBookId(bookId);
            record.setBorrowDate(today);
            record.setDueDate(today.plusDays(DEFAULT_LOAN_DAYS));
            record.setRenewCount(0);
            record.setStatus(BorrowStatusEnum.BORROWED);
            record.setFineAmount(BigDecimal.ZERO);
            borrowRecordMapper.insert(record);

            log.info("借书成功: userId={}, bookId={}, borrowId={}, dueDate={}",
                    userId, bookId, record.getId(), record.getDueDate());

            // 9. 发布事件（ES 同步）
            eventPublisher.publishEvent(new BookBorrowedEvent(bookId));

            BorrowResultVO result = BorrowResultVO.builder()
                    .borrowId(record.getId())
                    .bookTitle(book.getTitle())
                    .dueDate(record.getDueDate())
                    .status(record.getStatus().name())
                    .build();

            // 10. 锁延迟至事务提交后释放（@Transactional 提交发生在方法返回之后；
            //     若在 finally 内提前释放，其他线程可能在提交窗口内抢锁读到旧库存）
            deferLockRelease(lockKey);
            return result;
        } catch (Exception e) {
            // 异常：事务将回滚，立即释放锁
            releaseLockSafely(lockKey);
            throw e;
        }
    }

    /**
     * 延迟释放 Redis 锁至事务提交后.
     * <p>
     * 事务激活时注册 {@link TransactionSynchronization} 的 AFTER_COMMIT 回调，
     * 确保锁覆盖事务提交窗口；无事务上下文（如纯 Mockito 单测）时立即释放，保持单测行为不变。
     *
     * @param lockKey 锁键
     */
    private void deferLockRelease(String lockKey) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    releaseLockSafely(lockKey);
                }
            });
        } else {
            // 无事务上下文（单测场景），立即释放
            releaseLockSafely(lockKey);
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
            log.warn("释放 Redis 锁失败: key={}, error={}", lockKey, e.getMessage());
        }
    }

    // ==================== 还书 ====================

    @Override
    @Transactional
    public BorrowRecordVO returnBook(Long borrowId, Long userId) {
        // 1. 查借阅记录并校验归属
        BorrowRecord record = borrowRecordMapper.selectById(borrowId);
        if (record == null) {
            throw new BizException(ErrorCode.BORROW_RECORD_NOT_FOUND);
        }
        if (!record.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        if (record.getStatus() == BorrowStatusEnum.RETURNED) {
            throw new BizException(ErrorCode.BOOK_ALREADY_RETURNED);
        }

        // 2. 查图书并恢复库存
        Book book = bookMapper.selectById(record.getBookId());
        if (book != null) {
            book.setAvailCopies(book.getAvailCopies() + 1);
            // 乐观锁防护（与借书对称）：version 冲突时刷新实体重试一次，仍失败抛 CONFLICT
            if (bookMapper.updateById(book) == 0) {
                Book fresh = bookMapper.selectById(record.getBookId());
                if (fresh != null) {
                    fresh.setAvailCopies(fresh.getAvailCopies() + 1);
                    if (bookMapper.updateById(fresh) == 0) {
                        log.warn("还书恢复库存乐观锁冲突（重试仍失败）: bookId={}", record.getBookId());
                        throw new BizException(ErrorCode.CONFLICT);
                    }
                }
            }
        } else {
            log.warn("归还时图书不存在（可能已被删除）: borrowId={}, bookId={}", borrowId, record.getBookId());
        }

        // 3. 计算超期罚款（按实际归还日期计算，覆盖 OVERDUE 状态）
        LocalDate today = LocalDate.now();
        BigDecimal fine = BigDecimal.ZERO;
        if (today.isAfter(record.getDueDate())) {
            long overdueDays = ChronoUnit.DAYS.between(record.getDueDate(), today);
            if (overdueDays > 0) {
                fine = DAILY_FINE.multiply(BigDecimal.valueOf(overdueDays));

                // 查询是否已有 OverdueCheckJob 生成的 FineRecord（行级锁防并发）
                FineRecord existingFine = fineRecordMapper.selectOne(
                        new LambdaQueryWrapper<FineRecord>()
                                .eq(FineRecord::getBorrowId, borrowId)
                                .last("FOR UPDATE")
                );

                if (existingFine != null) {
                    // 更新为实际应缴金额
                    existingFine.setAmount(fine);
                    existingFine.setReason("超期 " + overdueDays + " 天（含已结算），日罚款 0.5 元");
                    fineRecordMapper.updateById(existingFine);
                    log.info("罚款已更新: borrowId={}, overdueDays={}, amount={}", borrowId, overdueDays, fine);
                } else {
                    // 新生成罚款记录
                    FineRecord fineRecord = new FineRecord();
                    fineRecord.setBorrowId(borrowId);
                    fineRecord.setAmount(fine);
                    fineRecord.setReason("超期 " + overdueDays + " 天，日罚款 0.5 元");
                    fineRecord.setPaid(0);
                    fineRecordMapper.insert(fineRecord);
                    log.info("罚款已生成: borrowId={}, overdueDays={}, amount={}", borrowId, overdueDays, fine);
                }
            }
        }

        // 4. 更新借阅记录
        record.setStatus(BorrowStatusEnum.RETURNED);
        record.setReturnDate(today);
        record.setFineAmount(fine);
        borrowRecordMapper.updateById(record);

        log.info("还书成功: borrowId={}, bookId={}, userId={}, fine={}",
                borrowId, record.getBookId(), record.getUserId(), fine);

        // 5. 发布事件（ES 同步 + 触发预约通知）
        eventPublisher.publishEvent(new BookReturnedEvent(record.getBookId()));

        return toRecordVO(record);
    }

    // ==================== 续借 ====================

    @Override
    @Transactional
    public RenewResultVO renew(Long borrowId, Long userId) {
        // 1. 查借阅记录并校验归属
        BorrowRecord record = borrowRecordMapper.selectById(borrowId);
        if (record == null) {
            throw new BizException(ErrorCode.BORROW_RECORD_NOT_FOUND);
        }
        if (!record.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }

        // 2. 校验续借次数
        if (record.getRenewCount() >= 1) {
            throw new BizException(ErrorCode.RENEW_LIMIT_EXCEEDED);
        }

        // 3. 校验未超期
        if (LocalDate.now().isAfter(record.getDueDate())) {
            throw new BizException(ErrorCode.RENEW_OVERDUE);
        }

        // 4. 校验未被预约（WAITING / NOTIFIED / RESERVED 任一活跃状态均不可续借）
        long waitingReservations = reservationMapper.selectCount(
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getBookId, record.getBookId())
                        .in(Reservation::getStatus,
                                ReservationStatusEnum.WAITING,
                                ReservationStatusEnum.NOTIFIED,
                                ReservationStatusEnum.RESERVED)
        );
        if (waitingReservations > 0) {
            throw new BizException(ErrorCode.RENEW_RESERVED);
        }

        // 5. 执行续借
        LocalDate oldDueDate = record.getDueDate();
        LocalDate newDueDate = oldDueDate.plusDays(RENEW_EXTEND_DAYS);

        record.setDueDate(newDueDate);
        record.setRenewCount(record.getRenewCount() + 1);
        record.setStatus(BorrowStatusEnum.RENEWED);
        borrowRecordMapper.updateById(record);

        log.info("续借成功: borrowId={}, userId={}, oldDue={}, newDue={}",
                borrowId, record.getUserId(), oldDueDate, newDueDate);

        return RenewResultVO.builder()
                .borrowId(borrowId)
                .oldDueDate(oldDueDate)
                .newDueDate(newDueDate)
                .renewCount(record.getRenewCount())
                .build();
    }

    // ==================== 查询 ====================

    @Override
    public PageResult<BorrowRecordVO> getMyBorrows(Long userId, String status, PageDTO pageDTO) {
        LambdaQueryWrapper<BorrowRecord> wrapper = new LambdaQueryWrapper<BorrowRecord>()
                .eq(BorrowRecord::getUserId, userId)
                .orderByDesc(BorrowRecord::getCreateTime);

        if (status != null && !status.isBlank()) {
            try {
                BorrowStatusEnum statusEnum = BorrowStatusEnum.valueOf(status.toUpperCase());
                wrapper.eq(BorrowRecord::getStatus, statusEnum);
            } catch (IllegalArgumentException e) {
                log.debug("无效的借阅状态筛选参数: {}", status);
            }
        }

        Page<BorrowRecord> page = new Page<>(pageDTO.getPageNum(), pageDTO.getPageSize());
        IPage<BorrowRecord> result = borrowRecordMapper.selectPage(page, wrapper);

        // 批量转换：一次查询关联图书，消除 N+1
        List<BorrowRecordVO> records = toRecordVOs(result.getRecords());

        return PageResult.of(records, result.getTotal(), pageDTO.getPageNum(), pageDTO.getPageSize());
    }

    @Override
    public BorrowRecordVO getBorrowDetail(Long borrowId, Long userId) {
        BorrowRecord record = borrowRecordMapper.selectById(borrowId);
        if (record == null) {
            throw new BizException(ErrorCode.BORROW_RECORD_NOT_FOUND);
        }
        if (!record.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        return toRecordVO(record);
    }

    @Override
    public PageResult<BorrowRecordVO> getHistory(Long userId, Integer year, PageDTO pageDTO) {
        LambdaQueryWrapper<BorrowRecord> wrapper = new LambdaQueryWrapper<BorrowRecord>()
                .eq(BorrowRecord::getUserId, userId)
                .in(BorrowRecord::getStatus, BorrowStatusEnum.RETURNED, BorrowStatusEnum.OVERDUE)
                .orderByDesc(BorrowRecord::getCreateTime);

        if (year != null) {
            // 使用日期范围而非 YEAR() 函数，避免函数式条件导致索引失效（便于利用 user_id 索引定位后范围比较）
            wrapper.between(BorrowRecord::getBorrowDate,
                    LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
        }

        Page<BorrowRecord> page = new Page<>(pageDTO.getPageNum(), pageDTO.getPageSize());
        IPage<BorrowRecord> result = borrowRecordMapper.selectPage(page, wrapper);

        // 批量转换：一次查询关联图书，消除 N+1
        List<BorrowRecordVO> records = toRecordVOs(result.getRecords());

        return PageResult.of(records, result.getTotal(), pageDTO.getPageNum(), pageDTO.getPageSize());
    }

    @Override
    public PageResult<BorrowRecordVO> getOverdueRecords(PageDTO pageDTO) {
        LambdaQueryWrapper<BorrowRecord> wrapper = new LambdaQueryWrapper<BorrowRecord>()
                .eq(BorrowRecord::getStatus, BorrowStatusEnum.OVERDUE)
                .orderByDesc(BorrowRecord::getCreateTime);

        Page<BorrowRecord> page = new Page<>(pageDTO.getPageNum(), pageDTO.getPageSize());
        IPage<BorrowRecord> result = borrowRecordMapper.selectPage(page, wrapper);

        // 批量转换：一次查询关联图书，消除 N+1
        List<BorrowRecordVO> records = toRecordVOs(result.getRecords());

        return PageResult.of(records, result.getTotal(), pageDTO.getPageNum(), pageDTO.getPageSize());
    }

    // ==================== VO 转换 ====================

    /**
     * 批量转换：一次查询所有关联图书，消除分页场景的 N+1 查询.
     *
     * @param records 借阅记录列表
     * @return 借阅记录 VO 列表
     */
    private List<BorrowRecordVO> toRecordVOs(List<BorrowRecord> records) {
        if (records.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> bookIds = records.stream()
                .map(BorrowRecord::getBookId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, BookSimpleVO> bookMap = loadBookMap(bookIds);
        return records.stream()
                .map(r -> buildRecordVO(r, bookMap.get(r.getBookId())))
                .toList();
    }

    /**
     * 单条转换（详情/归还等单条场景）.
     *
     * @param record 借阅记录
     * @return 借阅记录 VO
     */
    private BorrowRecordVO toRecordVO(BorrowRecord record) {
        Set<Long> ids = record.getBookId() != null
                ? Set.of(record.getBookId())
                : Collections.emptySet();
        return buildRecordVO(record, loadBookMap(ids).get(record.getBookId()));
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
     * Entity + 关联 BookSimpleVO → BorrowRecordVO.
     */
    private BorrowRecordVO buildRecordVO(BorrowRecord record, BookSimpleVO bookVO) {
        return BorrowRecordVO.builder()
                .id(record.getId())
                .userId(record.getUserId())
                .book(bookVO)
                .borrowDate(record.getBorrowDate())
                .dueDate(record.getDueDate())
                .returnDate(record.getReturnDate())
                .renewCount(record.getRenewCount())
                .status(record.getStatus() != null ? record.getStatus().name() : null)
                .fineAmount(record.getFineAmount())
                .build();
    }
}
