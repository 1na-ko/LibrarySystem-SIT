package com.library.security.controller;

import com.library.common.exception.BizException;
import com.library.common.exception.ErrorCode;
import com.library.core.dto.BookCreateDTO;
import com.library.core.dto.BookUpdateDTO;
import com.library.core.entity.Book;
import com.library.core.mapper.BookMapper;
import com.library.core.mapper.BorrowRecordMapper;
import com.library.core.service.BookService;
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
 * AdminBookController 单元测试.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@DisplayName("AdminBookController")
@ExtendWith(MockitoExtension.class)
class AdminBookControllerTest {

    @Mock
    private BookMapper bookMapper;

    @Mock
    private BorrowRecordMapper borrowRecordMapper;

    @Mock
    private BookService bookService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AdminBookController adminBookController;

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
    @DisplayName("POST /admin/books")
    class Create {

        @Test
        @DisplayName("ISBN 已存在时应抛出 DUPLICATE_ISBN")
        void shouldThrowWhenIsbnExists() {
            BookCreateDTO dto = BookCreateDTO.builder()
                    .isbn("978-7-111-58680-7").title("测试").author("作者")
                    .categoryId(1L).totalCopies(5).build();
            when(bookMapper.selectCount(any())).thenReturn(1L);

            assertThatThrownBy(() -> adminBookController.create(dto))
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

            var result = adminBookController.create(dto);

            assertThat(result.getCode()).isEqualTo(200);
            assertThat(result.getData().getTitle()).isEqualTo("深入理解Java虚拟机");
            assertThat(result.getData().getCategoryName()).isEqualTo("计算机科学");
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

            adminBookController.create(dto);
        }
    }

    @Nested
    @DisplayName("PUT /admin/books/{id}")
    class Update {

        @Test
        @DisplayName("图书不存在时应抛出 BOOK_NOT_FOUND")
        void shouldThrowWhenBookNotFound() {
            BookUpdateDTO dto = BookUpdateDTO.builder().title("新书名").build();
            when(bookMapper.selectById(999L)).thenReturn(null);

            assertThatThrownBy(() -> adminBookController.update(999L, dto))
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

            var result = adminBookController.update(1L, dto);

            assertThat(result.getCode()).isEqualTo(200);
            assertThat(result.getData().getCategoryName()).isEqualTo("计算机科学");
        }

        @Test
        @DisplayName("乐观锁冲突（updateById 返回 0）应抛出 CONFLICT")
        void shouldThrowOnOptimisticLockConflict() {
            BookUpdateDTO dto = BookUpdateDTO.builder().title("新书名").build();
            when(bookMapper.selectById(1L)).thenReturn(sampleBook);
            when(bookMapper.updateById(any(Book.class))).thenReturn(0);

            assertThatThrownBy(() -> adminBookController.update(1L, dto))
                    .isInstanceOf(BizException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.CONFLICT);
        }
    }

    @Nested
    @DisplayName("DELETE /admin/books/{id}")
    class Delete {

        @Test
        @DisplayName("图书不存在时应抛出 BOOK_NOT_FOUND")
        void shouldThrowWhenBookNotFound() {
            when(bookMapper.selectById(999L)).thenReturn(null);

            assertThatThrownBy(() -> adminBookController.delete(999L))
                    .isInstanceOf(BizException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.BOOK_NOT_FOUND);
        }

        @Test
        @DisplayName("存在活跃借阅时应抛出 CONFLICT")
        void shouldRefuseWhenActiveBorrows() {
            when(bookMapper.selectById(1L)).thenReturn(sampleBook);
            when(borrowRecordMapper.selectCount(any())).thenReturn(1L);

            assertThatThrownBy(() -> adminBookController.delete(1L))
                    .isInstanceOf(BizException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.CONFLICT);
            verify(bookMapper, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("删除成功应返回 200（MyBatis-Plus 自动逻辑删除）")
        void shouldDeleteAndReturn200() {
            when(bookMapper.selectById(1L)).thenReturn(sampleBook);
            when(borrowRecordMapper.selectCount(any())).thenReturn(0L);
            when(bookMapper.deleteById(1L)).thenReturn(1);

            var result = adminBookController.delete(1L);

            assertThat(result.getCode()).isEqualTo(200);
        }
    }
}
