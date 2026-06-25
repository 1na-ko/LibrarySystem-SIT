package com.library.core.service;

import com.library.common.exception.BizException;
import com.library.common.exception.ErrorCode;
import com.library.core.dto.BookCreateDTO;
import com.library.core.dto.BookUpdateDTO;
import com.library.core.entity.Book;
import com.library.core.event.BookCreatedEvent;
import com.library.core.event.BookDeletedEvent;
import com.library.core.event.BookUpdatedEvent;
import com.library.core.mapper.BookMapper;
import com.library.core.mapper.BorrowRecordMapper;
import com.library.core.service.impl.BookAdminServiceImpl;
import com.library.core.vo.BookDetailVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * BookAdminService 单元测试.
 * <p>
 * 覆盖管理端图书编目的业务逻辑：ISBN 唯一校验、活跃借阅检查、乐观锁、领域事件发布。
 * （阶段 6 审计修复：本测试由 security/AdminBookControllerTest 迁移而来，
 *  因编目逻辑已由 Controller 下沉至 BookAdminServiceImpl。）
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@DisplayName("BookAdminService")
@ExtendWith(MockitoExtension.class)
class BookAdminServiceTest {

    @Mock
    private BookMapper bookMapper;

    @Mock
    private BorrowRecordMapper borrowRecordMapper;

    @Mock
    private BookService bookService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private BookAdminServiceImpl bookAdminService;

    private Book sampleBook;
    private BookDetailVO sampleDetailVO;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        sampleBook = new Book();
        sampleBook.setId(1L);
        sampleBook.setIsbn("978-7-111-58680-7");
        sampleBook.setTitle("深入理解Java虚拟机");
        sampleBook.setAuthor("周志明");
        sampleBook.setPublisher("机械工业出版社");
        sampleBook.setPubDate(LocalDate.of(2019, 12, 1));
        sampleBook.setCategoryId(1L);
        sampleBook.setTotalCopies(5);
        sampleBook.setAvailCopies(3);
        sampleBook.setKeywords("Java,JVM,虚拟机");
        sampleBook.setBorrowCount(42);
        sampleBook.setVersion(1);
        sampleBook.setDeleted(0);
        sampleBook.setCreateTime(now);
        sampleBook.setUpdateTime(now);

        sampleDetailVO = BookDetailVO.builder()
                .id(1L)
                .isbn("978-7-111-58680-7")
                .title("深入理解Java虚拟机")
                .author("周志明")
                .publisher("机械工业出版社")
                .pubDate(LocalDate.of(2019, 12, 1))
                .categoryId(1L)
                .categoryName("计算机科学")
                .totalCopies(5)
                .availCopies(3)
                .keywordList(List.of("Java", "JVM", "虚拟机"))
                .borrowCount(42)
                .build();
    }

    @Nested
    @DisplayName("createBook")
    class Create {

        @Test
        @DisplayName("ISBN 已存在时应抛出 DUPLICATE_ISBN")
        void shouldThrowWhenIsbnExists() {
            BookCreateDTO dto = BookCreateDTO.builder()
                    .isbn("978-7-111-58680-7").title("测试").author("作者")
                    .categoryId(1L).totalCopies(5).build();
            when(bookMapper.selectCount(any())).thenReturn(1L);

            assertThatThrownBy(() -> bookAdminService.createBook(dto))
                    .isInstanceOf(BizException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.DUPLICATE_ISBN);
        }

        @Test
        @DisplayName("创建成功应返回 BookDetailVO（委托 BookService.getById）")
        void shouldCreateBookAndReturnDetail() {
            BookCreateDTO dto = BookCreateDTO.builder()
                    .isbn("978-7-111-58680-7").title("深入理解Java虚拟机").author("周志明")
                    .publisher("机械工业出版社").pubDate(LocalDate.of(2019, 12, 1))
                    .categoryId(1L).totalCopies(5).keywords("Java,JVM").build();

            when(bookMapper.selectCount(any())).thenReturn(0L);
            when(bookMapper.insert(any())).thenAnswer(inv -> {
                Book b = inv.getArgument(0);
                b.setId(1L);
                return 1;
            });
            when(bookService.getById(1L)).thenReturn(sampleDetailVO);

            BookDetailVO result = bookAdminService.createBook(dto);

            assertThat(result.getTitle()).isEqualTo("深入理解Java虚拟机");
            assertThat(result.getCategoryName()).isEqualTo("计算机科学");
            verify(eventPublisher).publishEvent(any(BookCreatedEvent.class));
        }

        @Test
        @DisplayName("新书 availCopies 应等于 totalCopies")
        void shouldSetAvailCopiesEqualToTotalCopies() {
            BookCreateDTO dto = BookCreateDTO.builder()
                    .isbn("978-7-111-58680-7").title("测试").author("作者")
                    .categoryId(1L).totalCopies(10).build();

            when(bookMapper.selectCount(any())).thenReturn(0L);
            when(bookMapper.insert(any())).thenAnswer(inv -> {
                Book b = inv.getArgument(0);
                assertThat(b.getAvailCopies()).isEqualTo(10);
                b.setId(1L);
                return 1;
            });
            when(bookService.getById(1L)).thenReturn(sampleDetailVO);

            bookAdminService.createBook(dto);
        }
    }

    @Nested
    @DisplayName("updateBook")
    class Update {

        @Test
        @DisplayName("图书不存在时应抛出 BOOK_NOT_FOUND")
        void shouldThrowWhenBookNotFound() {
            BookUpdateDTO dto = BookUpdateDTO.builder().title("新书名").build();
            when(bookMapper.selectById(999L)).thenReturn(null);

            assertThatThrownBy(() -> bookAdminService.updateBook(999L, dto))
                    .isInstanceOf(BizException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.BOOK_NOT_FOUND);
        }

        @Test
        @DisplayName("更新成功应委托 BookService.getById 返回详情")
        void shouldUpdateAndReturnDetail() {
            BookUpdateDTO dto = BookUpdateDTO.builder().title("深入理解Java虚拟机（第3版）").build();
            when(bookMapper.selectById(1L)).thenReturn(sampleBook);
            when(bookMapper.updateById(any(Book.class))).thenReturn(1);
            when(bookService.getById(1L)).thenReturn(sampleDetailVO);

            BookDetailVO result = bookAdminService.updateBook(1L, dto);

            assertThat(result.getCategoryName()).isEqualTo("计算机科学");
            verify(eventPublisher).publishEvent(any(BookUpdatedEvent.class));
        }

        @Test
        @DisplayName("乐观锁冲突（updateById 返回 0）应抛出 CONFLICT")
        void shouldThrowOnOptimisticLockConflict() {
            BookUpdateDTO dto = BookUpdateDTO.builder().title("新书名").build();
            when(bookMapper.selectById(1L)).thenReturn(sampleBook);
            when(bookMapper.updateById(any(Book.class))).thenReturn(0);

            assertThatThrownBy(() -> bookAdminService.updateBook(1L, dto))
                    .isInstanceOf(BizException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.CONFLICT);
        }
    }

    @Nested
    @DisplayName("deleteBook")
    class Delete {

        @Test
        @DisplayName("图书不存在时应抛出 BOOK_NOT_FOUND")
        void shouldThrowWhenBookNotFound() {
            when(bookMapper.selectById(999L)).thenReturn(null);

            assertThatThrownBy(() -> bookAdminService.deleteBook(999L))
                    .isInstanceOf(BizException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.BOOK_NOT_FOUND);
        }

        @Test
        @DisplayName("存在活跃借阅时应抛出 CONFLICT 且不删除")
        void shouldRefuseWhenActiveBorrows() {
            when(bookMapper.selectById(1L)).thenReturn(sampleBook);
            when(borrowRecordMapper.selectCount(any())).thenReturn(1L);

            assertThatThrownBy(() -> bookAdminService.deleteBook(1L))
                    .isInstanceOf(BizException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.CONFLICT);
            verify(bookMapper, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("删除成功应发布 BookDeletedEvent（MyBatis-Plus 自动逻辑删除）")
        void shouldDeleteAndPublishEvent() {
            when(bookMapper.selectById(1L)).thenReturn(sampleBook);
            when(borrowRecordMapper.selectCount(any())).thenReturn(0L);
            when(bookMapper.deleteById(1L)).thenReturn(1);

            bookAdminService.deleteBook(1L);

            verify(bookMapper).deleteById(1L);
            verify(eventPublisher).publishEvent(any(BookDeletedEvent.class));
        }
    }
}
