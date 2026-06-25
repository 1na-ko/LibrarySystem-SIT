package com.library.core.service;

import com.library.ai.llm.LlmService;
import com.library.ai.llm.LlmUnavailableException;
import com.library.core.config.RecommendationProperties;
import com.library.core.entity.Book;
import com.library.core.entity.BorrowRecord;
import com.library.core.entity.Category;
import com.library.core.mapper.BookMapper;
import com.library.core.mapper.BorrowRecordMapper;
import com.library.core.mapper.CategoryMapper;
import com.library.core.service.impl.RecommendationServiceImpl;
import com.library.core.vo.BookRecommendVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RecommendationService 单元测试.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@DisplayName("RecommendationService")
@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private CollaborativeFilteringService cfService;

    @Mock
    private ContentBasedService contentBasedService;

    @Mock
    private KGBasedRecommendService kgService;

    @Mock
    private BookMapper bookMapper;

    @Mock
    private BorrowRecordMapper borrowRecordMapper;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private RecommendationProperties properties;

    @Mock
    private LlmService llmService;

    private RecommendationServiceImpl recommendationService;

    private Book book1;
    private Book book2;
    private Book book3;
    private Category category;

    @BeforeEach
    void setUp() {
        lenient().when(properties.getCfWeight()).thenReturn(0.4);
        lenient().when(properties.getContentWeight()).thenReturn(0.3);
        lenient().when(properties.getKgWeight()).thenReturn(0.3);
        lenient().when(properties.getMaxLimit()).thenReturn(50);
        lenient().when(properties.getRecallTimeoutSeconds()).thenReturn(5L);

        book1 = buildBook(1L, "Java并发编程", "Brian Goetz", 1L, "Java,并发");
        book2 = buildBook(2L, "深入理解Java虚拟机", "周志明", 1L, "Java,JVM");
        book3 = buildBook(3L, "Effective Java", "Joshua Bloch", 1L, "Java");

        category = new Category();
        category.setId(1L);
        category.setName("计算机科学");

        // 构造器注入：推荐线程池用同步执行器（Runnable::run），单测中召回在当前线程立即执行
        recommendationService = new RecommendationServiceImpl(
                cfService, contentBasedService, kgService,
                bookMapper, borrowRecordMapper, categoryMapper,
                properties, Runnable::run, llmService);
    }

    @Nested
    @DisplayName("fusion")
    class Fusion {

        @Test
        @DisplayName("三路分数应按权重加权融合")
        void shouldMergeThreePathsWithCorrectWeights() {
            when(cfService.recommend(eq(1L), anyList())).thenReturn(Map.of(1L, 1.0));
            when(contentBasedService.recommend(anyLong(), anySet(), anyInt())).thenReturn(Map.of(1L, 0.5, 2L, 0.8));
            when(kgService.recommend(anyLong(), anyInt())).thenReturn(Collections.emptyMap());
            // 用户无借阅记录
            when(borrowRecordMapper.selectAllActiveForCF()).thenReturn(Collections.emptyList());
            // 批量加载图书
            when(bookMapper.selectBatchIds(any())).thenReturn(List.of(book2, book1));
            when(categoryMapper.selectBatchIds(List.of(1L))).thenReturn(List.of(category));

            List<BookRecommendVO> result = recommendationService.recommend(1L, 2);

            assertThat(result).hasSize(2);
            // 融合分数: book1=0.4*1.0+0.3*0.5=0.55, book2=0.3*0.8=0.24 → book1排第一
            assertThat(result.get(0).getBook().getId()).isEqualTo(1L);
            assertThat(result.get(1).getBook().getId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("应排除用户已借阅的图书")
        void shouldExcludeAlreadyBorrowedBooks() {
            when(cfService.recommend(eq(1L), anyList())).thenReturn(Map.of(1L, 1.0, 2L, 0.5));
            when(contentBasedService.recommend(anyLong(), anySet(), anyInt())).thenReturn(Collections.emptyMap());
            when(kgService.recommend(anyLong(), anyInt())).thenReturn(Collections.emptyMap());
            // 用户已借了书 1
            BorrowRecord record = new BorrowRecord();
            record.setUserId(1L);
            record.setBookId(1L);
            when(borrowRecordMapper.selectAllActiveForCF()).thenReturn(List.of(record));
            // 仅书 2 可推荐
            when(bookMapper.selectBatchIds(any())).thenReturn(List.of(book2));
            when(categoryMapper.selectBatchIds(List.of(1L))).thenReturn(List.of(category));

            List<BookRecommendVO> result = recommendationService.recommend(1L, 10);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getBook().getId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("应遵守 limit 参数")
        void shouldRespectLimit() {
            when(cfService.recommend(eq(1L), anyList())).thenReturn(Map.of(1L, 1.0, 2L, 0.9, 3L, 0.7));
            when(contentBasedService.recommend(anyLong(), anySet(), anyInt())).thenReturn(Collections.emptyMap());
            when(kgService.recommend(anyLong(), anyInt())).thenReturn(Collections.emptyMap());
            when(borrowRecordMapper.selectAllActiveForCF()).thenReturn(Collections.emptyList());
            when(bookMapper.selectBatchIds(any())).thenReturn(List.of(book1, book2));
            when(categoryMapper.selectBatchIds(List.of(1L))).thenReturn(List.of(category));

            List<BookRecommendVO> result = recommendationService.recommend(1L, 2);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("无候选时应返回空列表")
        void shouldReturnEmptyWhenAllPathsEmpty() {
            when(cfService.recommend(eq(1L), anyList())).thenReturn(Collections.emptyMap());
            when(contentBasedService.recommend(eq(1L), anySet(), eq(10))).thenReturn(Collections.emptyMap());
            when(kgService.recommend(1L, 10)).thenReturn(Collections.emptyMap());

            List<BookRecommendVO> result = recommendationService.recommend(1L, 10);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("LLM reason generation")
    class LlmReason {

        @BeforeEach
        void setUpFusion() {
            // 统一设置: CF 返回书 1 候选
            lenient().when(cfService.recommend(eq(1L), anyList())).thenReturn(Map.of(1L, 1.0));
            lenient().when(contentBasedService.recommend(anyLong(), anySet(), anyInt())).thenReturn(Collections.emptyMap());
            lenient().when(kgService.recommend(anyLong(), anyInt())).thenReturn(Collections.emptyMap());
            lenient().when(borrowRecordMapper.selectAllActiveForCF()).thenReturn(Collections.emptyList());
            lenient().when(bookMapper.selectBatchIds(any())).thenReturn(List.of(book1));
            lenient().when(categoryMapper.selectBatchIds(List.of(1L))).thenReturn(List.of(category));
        }

        @Test
        @DisplayName("LLM 可用时应生成个性化理由")
        void shouldGeneratePersonalizedReasonsWhenLLMAvailable() {
            List<Map<String, Object>> llmResponse = List.of(
                    Map.of("bookId", 1, "reason", "与您的技术栈高度契合，助力深入理解并发编程")
            );
            when(llmService.chat(any(), any(Class.class))).thenReturn(llmResponse);

            List<BookRecommendVO> result = recommendationService.recommend(1L, 10);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getReason()).contains("并发编程");
        }

        @Test
        @DisplayName("LLM 不可用时应降级为模板理由")
        void shouldFallbackToTemplateWhenLLMUnavailable() {
            when(llmService.chat(any(), any(Class.class)))
                    .thenThrow(new LlmUnavailableException("API unavailable"));

            List<BookRecommendVO> result = recommendationService.recommend(1L, 10);

            assertThat(result).hasSize(1);
            // 模板理由不应为空
            assertThat(result.get(0).getReason()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("cold start")
    class ColdStart {

        @Test
        @DisplayName("新用户（无推荐候选）应返回空列表")
        void shouldReturnEmptyList() {
            when(cfService.recommend(eq(999L), anyList())).thenReturn(Collections.emptyMap());
            when(contentBasedService.recommend(anyLong(), anySet(), anyInt())).thenReturn(Collections.emptyMap());
            when(kgService.recommend(anyLong(), anyInt())).thenReturn(Collections.emptyMap());

            List<BookRecommendVO> result = recommendationService.recommend(999L, 10);

            assertThat(result).isEmpty();
        }
    }

    private Book buildBook(Long id, String title, String author, Long categoryId, String keywords) {
        Book book = new Book();
        book.setId(id);
        book.setIsbn("978-7-" + id);
        book.setTitle(title);
        book.setAuthor(author);
        book.setPublisher("测试出版社");
        book.setCategoryId(categoryId);
        book.setKeywords(keywords);
        book.setBorrowCount(100);
        book.setAvailCopies(3);
        book.setTotalCopies(5);
        book.setVersion(1);
        book.setDeleted(0);
        return book;
    }
}
