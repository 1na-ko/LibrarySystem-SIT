package com.library.core.controller;

import com.library.common.result.PageResult;
import com.library.core.service.BookSearchService;
import com.library.core.service.BookService;
import com.library.core.service.RelatedBookService;
import com.library.core.vo.BookDetailVO;
import com.library.core.vo.BookRecommendVO;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * BookController 单元测试（独立 MockMvc，不启动 Spring 上下文）.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@DisplayName("BookController")
@ExtendWith(MockitoExtension.class)
class BookControllerTest {

    @Mock
    private BookSearchService bookSearchService;

    @Mock
    private BookService bookService;

    @Mock
    private RelatedBookService relatedBookService;

    @InjectMocks
    private BookController bookController;

    private MockMvc mockMvc;

    private BookDetailVO sampleDetail;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(bookController).build();

        sampleDetail = BookDetailVO.builder()
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
                .description("JVM经典")
                .borrowCount(127)
                .keywordsRaw("Java,JVM,虚拟机")
                .keywordList(List.of("Java", "JVM", "虚拟机"))
                .reservationCount(2)
                .build();
    }

    @Nested
    @DisplayName("GET /books/search")
    class Search {

        @Test
        @DisplayName("keyword 为空时应返回 400")
        void shouldReturn400WhenKeywordEmpty() throws Exception {
            mockMvc.perform(get("/books/search").param("keyword", ""))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("正常搜索应返回 200 + 分页结果")
        void shouldReturn200WhenSearchValid() throws Exception {
            BookSimpleVO vo = BookSimpleVO.builder()
                    .id(1L).title("深入理解Java虚拟机").author("周志明").availCopies(3).build();
            PageResult<BookSimpleVO> result = PageResult.of(List.of(vo), 1, 1, 20);

            when(bookSearchService.search(any())).thenReturn(result);

            mockMvc.perform(get("/books/search").param("keyword", "Java"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.records[0].title").value("深入理解Java虚拟机"));
        }
    }

    @Nested
    @DisplayName("GET /books/suggest")
    class Suggest {

        @Test
        @DisplayName("应返回补全建议列表")
        void shouldReturnSuggestions() throws Exception {
            List<SuggestVO> suggestions = List.of(
                    SuggestVO.builder().text("深入理解Java虚拟机").type("book").build()
            );
            when(bookSearchService.suggest(eq("深入"), anyInt())).thenReturn(suggestions);

            mockMvc.perform(get("/books/suggest").param("prefix", "深入"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].text").value("深入理解Java虚拟机"));
        }
    }

    @Nested
    @DisplayName("GET /books/hot")
    class HotBooks {

        @Test
        @DisplayName("应返回热门图书列表")
        void shouldReturnHotBooks() throws Exception {
            BookSimpleVO vo = BookSimpleVO.builder()
                    .id(1L).title("热门书").author("作者").availCopies(5).build();
            when(bookSearchService.hotBooks(any(), anyInt())).thenReturn(List.of(vo));

            mockMvc.perform(get("/books/hot").param("limit", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].title").value("热门书"));
        }
    }

    @Nested
    @DisplayName("GET /books/{id}")
    class GetDetail {

        @Test
        @DisplayName("应返回图书详情含预约人数（委托 BookService.getDetail）")
        void shouldReturnBookDetailWithReservationCount() throws Exception {
            // getDetail() 的 relatedBooks 组装已下沉至 BookServiceImpl，Controller 不再直接调用 RelatedBookService
            when(bookService.getDetail(1L)).thenReturn(sampleDetail);

            mockMvc.perform(get("/books/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.title").value("深入理解Java虚拟机"))
                    .andExpect(jsonPath("$.data.keywords[0]").value("Java"))
                    .andExpect(jsonPath("$.data.reservationCount").value(2));
        }
    }

    @Nested
    @DisplayName("GET /books/{id}/related")
    class GetRelated {

        @Test
        @DisplayName("应返回相关图书列表")
        void shouldReturnRelatedBooks() throws Exception {
            BookSimpleVO relatedBook = BookSimpleVO.builder()
                    .id(2L).title("Java并发编程实战").author("Brian Goetz").availCopies(2).build();
            BookRecommendVO rec = BookRecommendVO.builder()
                    .book(relatedBook).score(0.7).reason("同分类图书").build();
            when(relatedBookService.getRelated(eq(1L), anyInt())).thenReturn(List.of(rec));

            mockMvc.perform(get("/books/1/related").param("limit", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].book.title").value("Java并发编程实战"))
                    .andExpect(jsonPath("$.data[0].score").value(0.7));
        }
    }
}
