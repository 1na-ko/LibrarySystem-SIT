package com.library.core.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.common.exception.BizException;
import com.library.common.exception.ErrorCode;
import com.library.core.entity.Book;
import com.library.core.entity.BorrowRecord;
import com.library.core.entity.SysUser;
import com.library.core.enums.BorrowStatusEnum;
import com.library.core.enums.RoleEnum;
import com.library.core.enums.UserStatusEnum;
import com.library.core.event.BookBorrowedEvent;
import com.library.core.mapper.BookMapper;
import com.library.core.mapper.BorrowRecordMapper;
import com.library.core.mapper.FineRecordMapper;
import com.library.core.mapper.ReservationMapper;
import com.library.core.mapper.SysUserMapper;
import com.library.core.service.impl.BorrowServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * BorrowService 单元测试.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@DisplayName("BorrowService")
@ExtendWith(MockitoExtension.class)
class BorrowServiceTest {

    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private BookMapper bookMapper;
    @Mock
    private BorrowRecordMapper borrowRecordMapper;
    @Mock
    private ReservationMapper reservationMapper;
    @Mock
    private FineRecordMapper fineRecordMapper;
    @Mock
    private BookService bookService;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private BorrowServiceImpl borrowService;

    private SysUser user;
    private Book book;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();

        user = new SysUser();
        user.setId(1L);
        user.setUsername("test001");
        user.setStatus(UserStatusEnum.ACTIVE);
        user.setMaxBooks(5);
        user.setRole(RoleEnum.STUDENT);

        book = new Book();
        book.setId(10L);
        book.setTitle("深入理解Java虚拟机");
        book.setIsbn("978-7-111-58680-7");
        book.setAvailCopies(3);
        book.setTotalCopies(5);
        book.setBorrowCount(10);
        book.setVersion(1);

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Nested
    @DisplayName("borrow")
    class Borrow {

        @Test
        @DisplayName("正常借书时应返回 BorrowResultVO")
        void shouldReturnBorrowResultWhenBorrowSuccess() {
            when(sysUserMapper.selectById(1L)).thenReturn(user);
            when(bookMapper.selectById(10L)).thenReturn(book);
            when(borrowRecordMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(true);
            when(bookMapper.updateById(any(Book.class))).thenReturn(1);
            when(borrowRecordMapper.insert(any(BorrowRecord.class))).thenReturn(1);
            when(redisTemplate.delete(anyString())).thenReturn(true);

            var result = borrowService.borrow(1L, 10L);

            assertThat(result).isNotNull();
            assertThat(result.getBookTitle()).isEqualTo("深入理解Java虚拟机");
            assertThat(result.getStatus()).isEqualTo("BORROWED");
            assertThat(result.getDueDate()).isEqualTo(LocalDate.now().plusDays(30));
            verify(eventPublisher).publishEvent(any(BookBorrowedEvent.class));
        }

        @Test
        @DisplayName("用户不存在时应抛出 USER_NOT_FOUND")
        void shouldThrowUserNotFoundWhenUserNotExists() {
            when(sysUserMapper.selectById(999L)).thenReturn(null);

            assertThatThrownBy(() -> borrowService.borrow(999L, 10L))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("用户不存在");
        }

        @Test
        @DisplayName("账户冻结时应抛出 ACCOUNT_FROZEN")
        void shouldThrowAccountFrozenWhenUserFrozen() {
            user.setStatus(UserStatusEnum.FROZEN);
            when(sysUserMapper.selectById(1L)).thenReturn(user);

            assertThatThrownBy(() -> borrowService.borrow(1L, 10L))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("冻结");
        }

        @Test
        @DisplayName("库存为 0 时应抛出 BOOK_STOCK_EMPTY")
        void shouldThrowStockEmptyWhenNoCopies() {
            book.setAvailCopies(0);
            when(sysUserMapper.selectById(1L)).thenReturn(user);
            when(bookMapper.selectById(10L)).thenReturn(book);

            assertThatThrownBy(() -> borrowService.borrow(1L, 10L))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("库存不足");
        }

        @Test
        @DisplayName("超出借阅上限时应抛出 BORROW_LIMIT_EXCEEDED")
        void shouldThrowBorrowLimitExceededWhenAtMax() {
            when(sysUserMapper.selectById(1L)).thenReturn(user);
            when(bookMapper.selectById(10L)).thenReturn(book);
            // 第一次调用：活跃借阅数（≥maxBooks）
            when(borrowRecordMapper.selectCount(any(LambdaQueryWrapper.class)))
                    .thenReturn(5L); // = maxBooks

            assertThatThrownBy(() -> borrowService.borrow(1L, 10L))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("借阅数量超限");
        }

        @Test
        @DisplayName("重复借阅时应抛出 ALREADY_BORROWED")
        void shouldThrowAlreadyBorrowedWhenDuplicate() {
            when(sysUserMapper.selectById(1L)).thenReturn(user);
            when(bookMapper.selectById(10L)).thenReturn(book);
            // 第一次 selectCount: activeBorrowCount=0 (< maxBooks)
            // 第二次 selectCount: duplicateCount=1
            when(borrowRecordMapper.selectCount(any(LambdaQueryWrapper.class)))
                    .thenReturn(0L)   // activeBorrowCount
                    .thenReturn(1L);  // duplicateCount

            assertThatThrownBy(() -> borrowService.borrow(1L, 10L))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("不可重复借阅");
        }

        @Test
        @DisplayName("有超期未还时应抛出 OVERDUE_UNRETURNED")
        void shouldThrowOverdueUnreturnedWhenOverdueExists() {
            when(sysUserMapper.selectById(1L)).thenReturn(user);
            when(bookMapper.selectById(10L)).thenReturn(book);
            when(borrowRecordMapper.selectCount(any(LambdaQueryWrapper.class)))
                    .thenReturn(0L)   // activeBorrowCount
                    .thenReturn(0L)   // duplicateCount
                    .thenReturn(1L);  // overdueCount

            assertThatThrownBy(() -> borrowService.borrow(1L, 10L))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("超期未还");
        }
    }

    @Nested
    @DisplayName("returnBook")
    class ReturnBook {

        private BorrowRecord record;

        @BeforeEach
        void setUpRecord() {
            record = new BorrowRecord();
            record.setId(100L);
            record.setUserId(1L);
            record.setBookId(10L);
            record.setBorrowDate(LocalDate.now().minusDays(10));
            record.setDueDate(LocalDate.now().plusDays(20));
            record.setStatus(BorrowStatusEnum.BORROWED);
            record.setFineAmount(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("正常还书时应返回含今日日期的 BorrowRecordVO")
        void shouldReturnRecordWhenReturnSuccess() {
            when(borrowRecordMapper.selectById(100L)).thenReturn(record);
            when(bookMapper.selectById(10L)).thenReturn(book);
            when(bookMapper.updateById(any(Book.class))).thenReturn(1);
            when(borrowRecordMapper.updateById(any(BorrowRecord.class))).thenReturn(1);

            var result = borrowService.returnBook(100L, 1L);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo("RETURNED");
            assertThat(result.getFineAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            verify(eventPublisher).publishEvent(any(com.library.core.event.BookReturnedEvent.class));
        }

        @Test
        @DisplayName("借阅记录不存在时应抛出 BORROW_RECORD_NOT_FOUND")
        void shouldThrowNotFoundWhenRecordNotExists() {
            when(borrowRecordMapper.selectById(999L)).thenReturn(null);

            assertThatThrownBy(() -> borrowService.returnBook(999L, 1L))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("借阅记录不存在");
        }

        @Test
        @DisplayName("已归还的书记录应抛出 BOOK_ALREADY_RETURNED")
        void shouldThrowAlreadyReturnedWhenStatusReturned() {
            record.setStatus(BorrowStatusEnum.RETURNED);
            when(borrowRecordMapper.selectById(100L)).thenReturn(record);

            assertThatThrownBy(() -> borrowService.returnBook(100L, 1L))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("已归还");
        }

        @Test
        @DisplayName("超期还书应生成罚款记录")
        void shouldGenerateFineWhenOverdue() {
            record.setDueDate(LocalDate.now().minusDays(5)); // 5天前到期
            when(borrowRecordMapper.selectById(100L)).thenReturn(record);
            when(bookMapper.selectById(10L)).thenReturn(book);
            when(bookMapper.updateById(any(Book.class))).thenReturn(1);
            when(fineRecordMapper.insert(any())).thenReturn(1);
            when(borrowRecordMapper.updateById(any(BorrowRecord.class))).thenReturn(1);

            var result = borrowService.returnBook(100L, 1L);

            assertThat(result.getFineAmount()).isEqualByComparingTo(new BigDecimal("2.50"));
            verify(fineRecordMapper).insert(any());
        }
    }

    @Nested
    @DisplayName("renew")
    class Renew {

        private BorrowRecord record;

        @BeforeEach
        void setUpRecord() {
            record = new BorrowRecord();
            record.setId(100L);
            record.setUserId(1L);
            record.setBookId(10L);
            record.setDueDate(LocalDate.now().plusDays(10));
            record.setRenewCount(0);
            record.setStatus(BorrowStatusEnum.BORROWED);
        }

        @Test
        @DisplayName("正常续借时应返回 RenewResultVO 且 dueDate 延长 30 天")
        void shouldExtendDueDateWhenRenewSuccess() {
            LocalDate oldDueDate = record.getDueDate();
            when(borrowRecordMapper.selectById(100L)).thenReturn(record);
            when(reservationMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(borrowRecordMapper.updateById(any(BorrowRecord.class))).thenReturn(1);

            var result = borrowService.renew(100L, 1L);

            assertThat(result.getOldDueDate()).isEqualTo(oldDueDate);
            assertThat(result.getNewDueDate()).isEqualTo(oldDueDate.plusDays(30));
            assertThat(result.getRenewCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("续借次数超限时应抛出 RENEW_LIMIT_EXCEEDED")
        void shouldThrowLimitExceededWhenAlreadyRenewed() {
            record.setRenewCount(1);
            when(borrowRecordMapper.selectById(100L)).thenReturn(record);

            assertThatThrownBy(() -> borrowService.renew(100L, 1L))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("续借次数已达上限");
        }

        @Test
        @DisplayName("超期续借时应抛出 RENEW_OVERDUE")
        void shouldThrowOverdueWhenDueDatePassed() {
            record.setDueDate(LocalDate.now().minusDays(1));
            when(borrowRecordMapper.selectById(100L)).thenReturn(record);

            assertThatThrownBy(() -> borrowService.renew(100L, 1L))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("超期图书不可续借");
        }

        @Test
        @DisplayName("被预约时续借应抛出 RENEW_RESERVED")
        void shouldThrowReservedWhenWaitingReservationExists() {
            when(borrowRecordMapper.selectById(100L)).thenReturn(record);
            when(reservationMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            assertThatThrownBy(() -> borrowService.renew(100L, 1L))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("已被其他读者预约");
        }
    }
}
