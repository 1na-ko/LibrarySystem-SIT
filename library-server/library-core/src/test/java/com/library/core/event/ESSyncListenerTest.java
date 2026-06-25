package com.library.core.event;

import com.library.core.entity.Book;
import com.library.core.entity.Category;
import com.library.core.mapper.BookMapper;
import com.library.core.mapper.CategoryMapper;
import com.library.core.repository.BookDocument;
import com.library.core.repository.BookESRepository;
import com.library.core.service.impl.BookSearchServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ESSyncListener 单元测试.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@DisplayName("ESSyncListener")
@ExtendWith(MockitoExtension.class)
class ESSyncListenerTest {

    @Mock
    private BookESRepository bookESRepository;

    @Mock
    private BookMapper bookMapper;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private BookSearchServiceImpl bookSearchService;

    @InjectMocks
    private ESSyncListener esSyncListener;

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
        book.setDescription("JVM经典");
        book.setKeywords("Java,JVM,虚拟机");
        book.setBorrowCount(42);
        book.setVersion(1);
        book.setDeleted(0);
        book.setCreateTime(now);
        book.setUpdateTime(now);
    }

    @Nested
    @DisplayName("buildDocument")
    class BuildDocument {

        @Test
        @DisplayName("应从 Book Entity 正确构建 BookDocument 含 suggest 列表")
        void shouldBuildDocumentWithSuggestList() {
            when(bookMapper.selectById(1L)).thenReturn(book);
            when(categoryMapper.selectById(1L)).thenReturn(category);

            BookDocument doc = esSyncListener.buildDocument(1L);

            assertThat(doc.getId()).isEqualTo(1L);
            assertThat(doc.getTitle()).isEqualTo("深入理解Java虚拟机");
            assertThat(doc.getCategoryName()).isEqualTo("计算机科学");
            assertThat(doc.getBorrowCount()).isEqualTo(42);
            // suggest 应含书名 + 作者 + 关键词拆分
            assertThat(doc.getSuggest()).contains(
                    "深入理解Java虚拟机",
                    "周志明",
                    "Java", "JVM", "虚拟机"
            );
        }

        @Test
        @DisplayName("图书不存在时应返回 null")
        void shouldReturnNullWhenBookNotFound() {
            when(bookMapper.selectById(999L)).thenReturn(null);

            BookDocument doc = esSyncListener.buildDocument(999L);

            assertThat(doc).isNull();
        }

        @Test
        @DisplayName("关键词为空时 suggest 仍含书名和作者")
        void shouldContainTitleAndAuthorWhenKeywordsEmpty() {
            book.setKeywords(null);
            when(bookMapper.selectById(1L)).thenReturn(book);
            when(categoryMapper.selectById(1L)).thenReturn(category);

            BookDocument doc = esSyncListener.buildDocument(1L);

            assertThat(doc.getSuggest()).contains("深入理解Java虚拟机", "周志明");
        }
    }

    @Nested
    @DisplayName("onBookEvent - 同步（created/updated/borrowed/returned）")
    class OnBookSync {

        @Test
        @DisplayName("created/updated 应同步图书到 ES 并清除搜索缓存")
        void shouldSyncBookToEsAndEvictCache() {
            when(bookMapper.selectById(1L)).thenReturn(book);
            when(categoryMapper.selectById(1L)).thenReturn(category);

            // 阶段10：消费 MQ 事件（bookId + routingKey），原 onBookCreated 语义不变
            esSyncListener.onBookEvent(1L, EventBusConstants.RK_BOOK_CREATED);

            ArgumentCaptor<BookDocument> captor = ArgumentCaptor.forClass(BookDocument.class);
            verify(bookESRepository).save(captor.capture());
            verify(bookSearchService).evictAllSearchCache();
            assertThat(captor.getValue().getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("borrowed 应同步图书到 ES 但不清搜索缓存（仅元数据未变）")
        void shouldSyncBookToEsButNotEvictCacheOnBorrowed() {
            when(bookMapper.selectById(1L)).thenReturn(book);
            when(categoryMapper.selectById(1L)).thenReturn(category);

            esSyncListener.onBookEvent(1L, EventBusConstants.RK_BOOK_BORROWED);

            verify(bookESRepository).save(org.mockito.ArgumentMatchers.any(BookDocument.class));
            // 借/还高频事件不清搜索缓存，避免缓存命中率塌陷
            org.mockito.Mockito.verify(bookSearchService, org.mockito.Mockito.never())
                    .evictAllSearchCache();
        }

        @Test
        @DisplayName("returned 应同步图书到 ES 但不清搜索缓存")
        void shouldSyncBookToEsButNotEvictCacheOnReturned() {
            when(bookMapper.selectById(1L)).thenReturn(book);
            when(categoryMapper.selectById(1L)).thenReturn(category);

            esSyncListener.onBookEvent(1L, EventBusConstants.RK_BOOK_RETURNED);

            verify(bookESRepository).save(org.mockito.ArgumentMatchers.any(BookDocument.class));
            org.mockito.Mockito.verify(bookSearchService, org.mockito.Mockito.never())
                    .evictAllSearchCache();
        }
    }

    @Nested
    @DisplayName("onBookEvent - 删除")
    class OnBookDeleted {

        @Test
        @DisplayName("应删除 ES 文档并清除搜索缓存")
        void shouldDeleteFromEsAndEvictCache() {
            // 阶段10：消费 MQ 事件（bookId + routingKey=book.deleted），原 onBookDeleted 语义不变
            esSyncListener.onBookEvent(1L, EventBusConstants.RK_BOOK_DELETED);

            verify(bookESRepository).delete(1L);
            verify(bookSearchService).evictAllSearchCache();
        }
    }
}
