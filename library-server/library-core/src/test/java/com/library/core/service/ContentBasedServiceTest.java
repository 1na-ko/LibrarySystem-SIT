package com.library.core.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.ai.embedding.EmbeddingService;
import com.library.core.config.RecommendationProperties;
import com.library.core.entity.Book;
import com.library.core.entity.BorrowRecord;
import com.library.core.mapper.BookMapper;
import com.library.core.mapper.BorrowRecordMapper;
import com.library.core.mapper.CategoryMapper;
import com.library.core.service.impl.ContentBasedServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * ContentBasedService 单元测试.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@DisplayName("ContentBasedService")
@ExtendWith(MockitoExtension.class)
class ContentBasedServiceTest {

    @Mock
    private BookMapper bookMapper;

    @Mock
    private BorrowRecordMapper borrowRecordMapper;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private RecommendationProperties properties;

    @Mock
    private EmbeddingService embeddingService;

    private ContentBasedServiceImpl contentBasedService;

    @BeforeEach
    void setUp() {
        lenient().when(properties.getContentCandidateLimit()).thenReturn(500);
        // 构造器注入（含 EmbeddingService mock）
        contentBasedService = new ContentBasedServiceImpl(
                bookMapper, borrowRecordMapper, categoryMapper, properties, embeddingService);
    }

    @Nested
    @DisplayName("recommend")
    class RecommendWithEmbedding {

        @Test
        @DisplayName("应基于向量相似度返回 Top-N 候选")
        void shouldReturnSimilarBooksViaEmbedding() {
            List<BorrowRecord> records = new ArrayList<>();
            records.add(buildRecord(1L, 1L));
            records.add(buildRecord(2L, 2L));

            Book book1 = buildBook(1L, "Java编程思想", "Java,编程");
            Book book2 = buildBook(2L, "Effective Java", "Java");

            when(borrowRecordMapper.selectAllActiveForCF()).thenReturn(records);
            when(bookMapper.selectBatchIds(any())).thenReturn(List.of(book1));
            when(bookMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(book2));

            List<Float> vec1 = List.of(1.0f, 1.0f, 1.0f);
            List<Float> vec2 = List.of(1.0f, 0.9f, 1.0f);
            when(embeddingService.batchEmbed(anyList()))
                    .thenReturn(List.of(vec1))
                    .thenReturn(List.of(vec2));

            Map<Long, Double> result = contentBasedService.recommend(1L, 5);

            assertThat(result).containsKey(2L);
            assertThat(result.get(2L)).isGreaterThan(0.5);
        }

        @Test
        @DisplayName("无借阅记录时应返回空")
        void shouldReturnEmptyWhenNoBorrowHistory() {
            List<BorrowRecord> records = new ArrayList<>();
            records.add(buildRecord(2L, 1L));

            when(borrowRecordMapper.selectAllActiveForCF()).thenReturn(records);

            Map<Long, Double> result = contentBasedService.recommend(1L, 10);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("用户画像应为已借图书向量的逐元素均值")
        void shouldBuildUserProfileAsMeanVector() {
            List<BorrowRecord> records = new ArrayList<>();
            records.add(buildRecord(1L, 1L));
            records.add(buildRecord(1L, 2L));

            Book book1 = buildBook(1L, "Java并发", "Java");
            Book book2 = buildBook(2L, "Spring实战", "Spring");
            Book book3 = buildBook(3L, "设计模式", "设计模式");

            when(borrowRecordMapper.selectAllActiveForCF()).thenReturn(records);
            when(bookMapper.selectBatchIds(any())).thenReturn(List.of(book1, book2));
            when(bookMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(book3));

            when(embeddingService.batchEmbed(anyList()))
                    .thenReturn(List.of(List.of(2.0f, 0.0f, 0.0f), List.of(0.0f, 2.0f, 0.0f)))
                    .thenReturn(List.of(List.of(1.0f, 0.0f, 1.0f)));

            Map<Long, Double> result = contentBasedService.recommend(1L, 5);

            assertThat(result).containsKey(3L);
        }
    }

    @Nested
    @DisplayName("recommend with null EmbeddingService")
    class RecommendWithoutEmbedding {

        @Test
        @DisplayName("EmbeddingService 为 null 时应返回空")
        void shouldReturnEmptyWhenEmbeddingServiceAbsent() {
            // 构造时传入 null
            ContentBasedServiceImpl serviceWithoutEmbedding = new ContentBasedServiceImpl(
                    bookMapper, borrowRecordMapper, categoryMapper, properties, null);

            Map<Long, Double> result = serviceWithoutEmbedding.recommend(1L, 10);

            assertThat(result).isEmpty();
        }
    }

    private Book buildBook(Long id, String title, String keywords) {
        Book book = new Book();
        book.setId(id);
        book.setIsbn("978-7-" + id);
        book.setTitle(title);
        book.setAuthor("作者" + id);
        book.setKeywords(keywords);
        book.setBorrowCount(10);
        book.setAvailCopies(3);
        book.setTotalCopies(5);
        book.setVersion(1);
        book.setDeleted(0);
        return book;
    }

    private BorrowRecord buildRecord(Long userId, Long bookId) {
        BorrowRecord record = new BorrowRecord();
        record.setUserId(userId);
        record.setBookId(bookId);
        return record;
    }
}
