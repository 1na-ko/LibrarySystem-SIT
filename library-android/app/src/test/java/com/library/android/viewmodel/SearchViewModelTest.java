package com.library.android.viewmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.library.android.model.BookSimpleVO;
import com.library.android.model.PageResult;
import com.library.android.model.SuggestVO;
import com.library.android.repository.BookRepository;
import com.library.android.testutil.PageResults;
import com.library.android.testutil.ResultFactory;
import com.library.android.testutil.RxJava3SchedulerRule;
import com.library.android.ui.common.LoadingState;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import io.reactivex.rxjava3.core.Single;

public class SearchViewModelTest {

    @Rule public InstantTaskExecutorRule instantTaskRule = new InstantTaskExecutorRule();
    @Rule public RxJava3SchedulerRule rxRule = new RxJava3SchedulerRule();

    private BookRepository repository;
    private SearchViewModel viewModel;

    @Before
    public void setUp() {
        repository = mock(BookRepository.class);
        viewModel = new SearchViewModel(repository);
    }

    @Test
    public void search_success_shouldExposeResultsAndMethodLabel() {
        BookSimpleVO b = mock(BookSimpleVO.class);
        when(repository.searchBooks(anyString(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(Single.just(ResultFactory.success(
                        PageResults.single(Collections.singletonList(b)))));

        viewModel.search("数据结构");

        assertNotNull(viewModel.getSearchResults().getValue());
        assertEquals(1, viewModel.getSearchResults().getValue().size());
        assertEquals("关键词搜索", viewModel.getSearchMethodLabel().getValue());
        assertEquals("数据结构", viewModel.getResultTitle().getValue());
    }

    @Test
    public void searchByCategory_success_shouldUseAdvancedSearchAndLabelAsCategory() {
        BookSimpleVO b = mock(BookSimpleVO.class);
        when(repository.advancedSearch(any(), any(), any(), any(), any(), any(), eq(7L), any(), anyInt(), anyInt()))
                .thenReturn(Single.just(ResultFactory.success(
                        PageResults.single(Arrays.asList(b, b)))));

        viewModel.searchByCategory(7L, "计算机");

        assertEquals(2, viewModel.getSearchResults().getValue().size());
        assertEquals("分类浏览", viewModel.getSearchMethodLabel().getValue());
        assertEquals("计算机", viewModel.getResultTitle().getValue());
    }

    @Test
    public void searchAdvanced_isbnOnly_shouldLabelAsIsbnSearch() {
        when(repository.advancedSearch(any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(Single.just(ResultFactory.success(
                        PageResults.single(Collections.singletonList(mock(BookSimpleVO.class))))));

        viewModel.searchAdvanced(null, null, "9787-xxx", null, null, null, null);

        assertEquals("ISBN搜索", viewModel.getSearchMethodLabel().getValue());
        assertEquals("9787-xxx", viewModel.getResultTitle().getValue());
    }

    @Test
    public void search_apiFailure_shouldPostError() {
        when(repository.searchBooks(anyString(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(Single.just(ResultFactory.failure(500, "boom")));

        viewModel.search("k");

        assertNotNull(viewModel.getErrorEvent().getValue());
    }

    @Test
    public void clearSearchState_shouldResetAllSearchData() {
        when(repository.searchBooks(anyString(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(Single.just(ResultFactory.success(
                        PageResults.single(Collections.singletonList(mock(BookSimpleVO.class))))));
        viewModel.search("k");
        viewModel.clearSearchState();

        assertNull(viewModel.getResultTitle().getValue());
        assertNull(viewModel.getSearchResults().getValue());
        assertEquals(Integer.valueOf(0), viewModel.getTotalResults().getValue());
    }

    @Test
    public void loadSuggestions_emptyPrefix_shouldNotCallRepository() {
        viewModel.loadSuggestions("   ");
        // suggestions 应被设为 null，不应调用 repo
        assertNull(viewModel.getSuggestions().getValue());
    }

    @Test
    public void loadSuggestions_validPrefix_shouldExposeSuggestions() {
        SuggestVO s = mock(SuggestVO.class);
        when(repository.getSuggestions(anyString(), anyInt()))
                .thenReturn(Single.just(ResultFactory.success(Collections.singletonList(s))));

        viewModel.loadSuggestions("数据");

        assertNotNull(viewModel.getSuggestions().getValue());
        assertEquals(1, viewModel.getSuggestions().getValue().size());
    }
}
