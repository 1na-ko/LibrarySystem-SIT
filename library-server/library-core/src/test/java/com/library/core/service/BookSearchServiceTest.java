package com.library.core.service;

import com.library.common.result.PageResult;
import com.library.core.dto.BookAdvancedSearchDTO;
import com.library.core.dto.BookSearchDTO;
import com.library.core.repository.BookESRepository;
import com.library.core.service.impl.BookSearchServiceImpl;
import com.library.core.vo.BookSimpleVO;
import com.library.core.vo.SuggestVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * BookSearchService 单元测试.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@DisplayName("BookSearchService")
@ExtendWith(MockitoExtension.class)
class BookSearchServiceTest {

    @Mock
    private BookESRepository bookESRepository;

    @Mock
    private BookService bookService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private BookSearchServiceImpl bookSearchService;

    private BookSimpleVO sampleBook;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        sampleBook = BookSimpleVO.builder()
                .id(1L)
                .isbn("978-7-111-58680-7")
                .title("深入理解Java虚拟机")
                .author("周志明")
                .publisher("机械工业出版社")
                .pubDate(LocalDate.of(2019, 12, 1))
                .availCopies(3)
                .build();

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Nested
    @DisplayName("search")
    class Search {

        @Test
        @DisplayName("缓存命中时应直接返回缓存结果")
        void shouldReturnCachedResultWhenCacheHit() {
            BookSearchDTO dto = BookSearchDTO.builder().keyword("Java").pageNum(1).pageSize(20).build();
            PageResult<BookSimpleVO> cached = PageResult.of(List.of(sampleBook), 1, 1, 20);

            when(valueOperations.get(anyString())).thenReturn(cached);

            PageResult<BookSimpleVO> result = bookSearchService.search(dto);

            assertThat(result.getRecords()).hasSize(1);
            assertThat(result.getRecords().get(0).getTitle()).isEqualTo("深入理解Java虚拟机");
            verify(bookESRepository, never()).fullTextSearch(anyString(), any(), any(), any(), anyInt(), anyInt());
        }

        @Test
        @DisplayName("缓存未命中时应查 ES 并回写缓存")
        void shouldSearchESAndWriteCacheWhenCacheMiss() {
            BookSearchDTO dto = BookSearchDTO.builder().keyword("Java").pageNum(1).pageSize(20).build();
            PageResult<Long> esResult = PageResult.of(List.of(1L), 1, 1, 20);

            when(valueOperations.get(anyString())).thenReturn(null);
            when(bookESRepository.fullTextSearch(eq("Java"), eq(null), eq(null), eq(null), eq(1), eq(20)))
                    .thenReturn(esResult);
            when(bookService.listByIds(List.of(1L))).thenReturn(List.of(sampleBook));

            PageResult<BookSimpleVO> result = bookSearchService.search(dto);

            assertThat(result.getRecords()).hasSize(1);
            verify(valueOperations).set(anyString(), any(), anyLong(), any());
        }

        @Test
        @DisplayName("自定义排序（borrowCount）时不应缓存")
        void shouldNotCacheWhenCustomSort() {
            BookSearchDTO dto = BookSearchDTO.builder().keyword("Java").sortBy("borrowCount").pageNum(1).pageSize(20).build();
            PageResult<Long> esResult = PageResult.of(List.of(1L), 1, 1, 20);

            when(bookESRepository.fullTextSearch(eq("Java"), eq(null), eq(null), eq("borrowCount"), eq(1), eq(20)))
                    .thenReturn(esResult);
            when(bookService.listByIds(List.of(1L))).thenReturn(List.of(sampleBook));

            bookSearchService.search(dto);

            verify(valueOperations, never()).set(anyString(), any(), anyLong(), any());
        }

        @Test
        @DisplayName("ES 搜索无结果时应返回空分页")
        void shouldReturnEmptyWhenNoResults() {
            BookSearchDTO dto = BookSearchDTO.builder().keyword("NonExistent").pageNum(1).pageSize(20).build();
            PageResult<Long> esResult = PageResult.of(List.of(), 0, 1, 20);

            when(valueOperations.get(anyString())).thenReturn(null);
            when(bookESRepository.fullTextSearch(anyString(), any(), any(), any(), anyInt(), anyInt()))
                    .thenReturn(esResult);

            PageResult<BookSimpleVO> result = bookSearchService.search(dto);

            assertThat(result.getRecords()).isEmpty();
            assertThat(result.getTotal()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("advancedSearch")
    class AdvancedSearch {

        @Test
        @DisplayName("高级搜索应委托 ES 并转换结果")
        void shouldDelegateToESAndConvert() {
            BookAdvancedSearchDTO dto = BookAdvancedSearchDTO.builder()
                    .title("Java").onlyAvailable(true).pageNum(1).pageSize(20).build();
            PageResult<Long> esResult = PageResult.of(List.of(1L), 1, 1, 20);

            when(bookESRepository.advancedSearch(dto)).thenReturn(esResult);
            when(bookService.listByIds(List.of(1L))).thenReturn(List.of(sampleBook));

            PageResult<BookSimpleVO> result = bookSearchService.advancedSearch(dto);

            assertThat(result.getRecords()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("suggest")
    class Suggest {

        @Test
        @DisplayName("非空前缀应返回补全建议")
        void shouldReturnSuggestionsWhenPrefixGiven() {
            when(bookESRepository.suggest("深入", 10)).thenReturn(List.of("深入理解Java虚拟机"));

            List<SuggestVO> result = bookSearchService.suggest("深入", 10);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getText()).isEqualTo("深入理解Java虚拟机");
            assertThat(result.get(0).getType()).isEqualTo("book");
        }

        @Test
        @DisplayName("空前缀应返回空列表")
        void shouldReturnEmptyWhenPrefixBlank() {
            List<SuggestVO> result = bookSearchService.suggest("", 10);

            assertThat(result).isEmpty();
            verify(bookESRepository, never()).suggest(anyString(), anyInt());
        }
    }

    @Nested
    @DisplayName("hotBooks")
    class HotBooks {

        @Test
        @DisplayName("应返回热门图书列表")
        void shouldReturnHotBooks() {
            when(bookESRepository.hotBooks(null, 10)).thenReturn(List.of(1L));
            when(bookService.listByIds(List.of(1L))).thenReturn(List.of(sampleBook));

            List<BookSimpleVO> result = bookSearchService.hotBooks(null, 10);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("ES 无热门图书时应返回空列表")
        void shouldReturnEmptyWhenNoHotBooks() {
            when(bookESRepository.hotBooks(null, 10)).thenReturn(List.of());

            List<BookSimpleVO> result = bookSearchService.hotBooks(null, 10);

            assertThat(result).isEmpty();
        }
    }
}
