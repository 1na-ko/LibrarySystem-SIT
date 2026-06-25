package com.library.core.service;

import com.library.common.exception.BizException;
import com.library.core.entity.Book;
import com.library.core.entity.Category;
import com.library.core.mapper.BookMapper;
import com.library.core.mapper.CategoryMapper;
import com.library.core.mapper.ReservationMapper;
import com.library.core.service.impl.BookServiceImpl;
import com.library.core.vo.BookDetailVO;
import com.library.core.vo.BookRecommendVO;
import com.library.core.vo.BookSimpleVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * BookService 单元测试.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@DisplayName("BookService")
@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookMapper bookMapper;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private ReservationMapper reservationMapper;

    @Mock
    private RelatedBookService relatedBookService;

    @InjectMocks
    private BookServiceImpl bookService;

    private Book book;
    private Category category;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        category = new Category();
        category.setId(1L);
        category.setName("计算机科学");

        book = new Book();
        book.setId(1L);
        book.setIsbn("978-7-111-58680-7");
        book.setTitle("深入理解Java虚拟机");
        book.setAuthor("周志明");
        book.setPublisher("机械工业出版社");
        book.setPubDate(LocalDate.of(2019, 12, 1));
        book.setCategoryId(1L);
        book.setTotalCopies(5);
        book.setAvailCopies(3);
        book.setDescription("Java虚拟机经典著作");
        book.setCoverUrl("https://example.com/cover.jpg");
        book.setLocation("A区-3架-12层");
        book.setKeywords("Java,JVM,虚拟机");
        book.setBorrowCount(42);
        book.setVersion(1);
        book.setDeleted(0);
        book.setCreateTime(now);
        book.setUpdateTime(now);
    }

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("ID 存在时应返回 BookDetailVO 含分类名")
        void shouldReturnBookDetailWhenIdExists() {
            when(bookMapper.selectById(1L)).thenReturn(book);
            when(categoryMapper.selectById(1L)).thenReturn(category);

            BookDetailVO vo = bookService.getById(1L);

            assertThat(vo.getTitle()).isEqualTo("深入理解Java虚拟机");
            assertThat(vo.getCategoryName()).isEqualTo("计算机科学");
            assertThat(vo.getAvailCopies()).isEqualTo(3);
            assertThat(vo.getBorrowCount()).isEqualTo(42);
        }

        @Test
        @DisplayName("ID 不存在时应抛出 BOOK_NOT_FOUND")
        void shouldThrowBizExceptionWhenBookNotFound() {
            when(bookMapper.selectById(999L)).thenReturn(null);

            assertThatThrownBy(() -> bookService.getById(999L))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("图书不存在");
        }
    }

    @Nested
    @DisplayName("getDetail")
    class GetDetail {

        @Test
        @DisplayName("应返回含 reservationCount 的 BookDetailVO")
        void shouldReturnDetailWithReservationCount() {
            when(bookMapper.selectById(1L)).thenReturn(book);
            when(categoryMapper.selectById(1L)).thenReturn(category);
            when(reservationMapper.selectCount(any())).thenReturn(3L);
            when(relatedBookService.getRelated(eq(1L), anyInt())).thenReturn(List.of());

            BookDetailVO vo = bookService.getDetail(1L);

            assertThat(vo.getTitle()).isEqualTo("深入理解Java虚拟机");
            assertThat(vo.getReservationCount()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("getByIsbn")
    class GetByIsbn {

        @Test
        @DisplayName("ISBN 存在时应返回 BookDetailVO")
        void shouldReturnBookDetailWhenIsbnExists() {
            when(bookMapper.selectOne(any())).thenReturn(book);
            when(categoryMapper.selectById(1L)).thenReturn(category);

            BookDetailVO vo = bookService.getByIsbn("978-7-111-58680-7");

            assertThat(vo).isNotNull();
            assertThat(vo.getIsbn()).isEqualTo("978-7-111-58680-7");
        }

        @Test
        @DisplayName("ISBN 不存在时应抛出 BOOK_NOT_FOUND")
        void shouldThrowBizExceptionWhenIsbnNotFound() {
            when(bookMapper.selectOne(any())).thenReturn(null);

            assertThatThrownBy(() -> bookService.getByIsbn("000-0-000-00000-0"))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("图书不存在");
        }
    }

    @Nested
    @DisplayName("listByIds")
    class ListByIds {

        @Test
        @DisplayName("传入 ID 列表时应返回对应 BookSimpleVO 列表")
        void shouldReturnBookSimpleListWhenIdsGiven() {
            when(bookMapper.selectBatchIds(List.of(1L))).thenReturn(List.of(book));

            List<BookSimpleVO> vos = bookService.listByIds(List.of(1L));

            assertThat(vos).hasSize(1);
            assertThat(vos.get(0).getTitle()).isEqualTo("深入理解Java虚拟机");
            assertThat(vos.get(0).getAvailCopies()).isEqualTo(3);
        }

        @Test
        @DisplayName("空列表或 null 时应返回空列表")
        void shouldReturnEmptyListWhenIdsEmpty() {
            assertThat(bookService.listByIds(List.of())).isEmpty();
            assertThat(bookService.listByIds(null)).isEmpty();
        }
    }
}
