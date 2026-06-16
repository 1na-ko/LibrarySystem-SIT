package com.library.core.service;

import com.library.common.exception.BizException;
import com.library.core.entity.Book;
import com.library.core.entity.Category;
import com.library.core.mapper.BookMapper;
import com.library.core.mapper.CategoryMapper;
import com.library.core.service.impl.RelatedBookServiceImpl;
import com.library.core.vo.BookRecommendVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * RelatedBookService 单元测试.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@DisplayName("RelatedBookService")
@ExtendWith(MockitoExtension.class)
class RelatedBookServiceTest {

    @Mock
    private BookMapper bookMapper;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private RelatedBookServiceImpl relatedBookService;

    private Book targetBook;
    private Book sameCategoryBook;
    private Book sameAuthorBook;
    private Category category1;
    private Category category2;

    @BeforeEach
    void setUp() {
        targetBook = buildBook(1L, "深入理解Java虚拟机", "周志明", 1L, 100);
        sameCategoryBook = buildBook(2L, "Java并发编程实战", "Brian Goetz", 1L, 80);
        sameAuthorBook = buildBook(3L, "深入理解计算机系统", "周志明", 2L, 60);

        category1 = new Category();
        category1.setId(1L);
        category1.setName("计算机科学");
        category2 = new Category();
        category2.setId(2L);
        category2.setName("操作系统");

        // 为 toRecommendVO 中的分类名称查詢提供默认 Mock
        lenient().when(categoryMapper.selectById(1L)).thenReturn(category1);
        lenient().when(categoryMapper.selectById(2L)).thenReturn(category2);
    }

    @Nested
    @DisplayName("getRelated")
    class GetRelated {

        @Test
        @DisplayName("应返回同分类图书（按 borrowCount 降序）")
        void shouldReturnSameCategoryBooks() {
            when(bookMapper.selectById(1L)).thenReturn(targetBook);
            when(bookMapper.selectList(any())).thenReturn(List.of(sameCategoryBook));

            List<BookRecommendVO> result = relatedBookService.getRelated(1L, 10);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getBook().getTitle()).isEqualTo("Java并发编程实战");
            assertThat(result.get(0).getScore()).isEqualTo(0.7);
            assertThat(result.get(0).getReason()).isEqualTo("同分类图书");
        }

        @Test
        @DisplayName("同分类不足时应补充同作者图书且去重")
        void shouldSupplementWithSameAuthorWhenCategoryInsufficient() {
            when(bookMapper.selectById(1L)).thenReturn(targetBook);
            when(bookMapper.selectList(any()))
                    .thenReturn(List.of(sameCategoryBook))
                    .thenReturn(List.of(sameAuthorBook, sameCategoryBook));

            List<BookRecommendVO> result = relatedBookService.getRelated(1L, 5);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getScore()).isEqualTo(0.7);
            assertThat(result.get(1).getScore()).isEqualTo(0.5);
        }

        @Test
        @DisplayName("应排除目标图书自身")
        void shouldExcludeTargetBook() {
            when(bookMapper.selectById(1L)).thenReturn(targetBook);
            when(bookMapper.selectList(any())).thenReturn(List.of(targetBook, sameCategoryBook));

            List<BookRecommendVO> result = relatedBookService.getRelated(1L, 10);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getBook().getId()).isNotEqualTo(1L);
        }

        @Test
        @DisplayName("不应超过 limit")
        void shouldRespectLimit() {
            Book extraBook = buildBook(4L, "Effective Java", "Joshua Bloch", 1L, 70);
            when(bookMapper.selectById(1L)).thenReturn(targetBook);
            when(bookMapper.selectList(any())).thenReturn(List.of(sameCategoryBook, extraBook));

            List<BookRecommendVO> result = relatedBookService.getRelated(1L, 1);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("图书不存在时应抛 BOOK_NOT_FOUND")
        void shouldThrowWhenBookNotFound() {
            when(bookMapper.selectById(999L)).thenReturn(null);

            assertThatThrownBy(() -> relatedBookService.getRelated(999L, 10))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("图书不存在");
        }

        @Test
        @DisplayName("无相关图书时应返回空列表")
        void shouldReturnEmptyWhenNoRelatedBooks() {
            when(bookMapper.selectById(1L)).thenReturn(targetBook);
            when(bookMapper.selectList(any())).thenReturn(List.of());

            List<BookRecommendVO> result = relatedBookService.getRelated(1L, 10);

            assertThat(result).isEmpty();
        }
    }

    private Book buildBook(Long id, String title, String author, Long categoryId, int borrowCount) {
        Book book = new Book();
        book.setId(id);
        book.setIsbn("978-7-" + id);
        book.setTitle(title);
        book.setAuthor(author);
        book.setPublisher("测试出版社");
        book.setPubDate(LocalDate.of(2020, 1, 1));
        book.setCategoryId(categoryId);
        book.setBorrowCount(borrowCount);
        book.setAvailCopies(3);
        book.setTotalCopies(5);
        book.setVersion(1);
        book.setDeleted(0);
        return book;
    }
}
