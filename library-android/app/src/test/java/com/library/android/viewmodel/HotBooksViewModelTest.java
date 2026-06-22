package com.library.android.viewmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.library.android.model.BookSimpleVO;
import com.library.android.model.CategoryVO;
import com.library.android.repository.BookRepository;
import com.library.android.testutil.ResultFactory;
import com.library.android.testutil.RxJava3SchedulerRule;
import com.library.android.ui.common.LoadingState;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;

import io.reactivex.rxjava3.core.Single;

public class HotBooksViewModelTest {

    @Rule public InstantTaskExecutorRule instantTaskRule = new InstantTaskExecutorRule();
    @Rule public RxJava3SchedulerRule rxRule = new RxJava3SchedulerRule();

    private BookRepository repository;
    private HotBooksViewModel viewModel;

    @Before
    public void setUp() {
        repository = mock(BookRepository.class);
        viewModel = new HotBooksViewModel(repository);
    }

    @Test
    public void loadCategories_success_shouldExposeCategories() {
        CategoryVO c = mock(CategoryVO.class);
        when(repository.getCategoryTree())
                .thenReturn(Single.just(ResultFactory.success(Collections.singletonList(c))));

        viewModel.loadCategories();

        assertNotNull(viewModel.getCategories().getValue());
        assertEquals(1, viewModel.getCategories().getValue().size());
    }

    @Test
    public void loadHotBooks_success_shouldUpdateContentState() {
        BookSimpleVO b = mock(BookSimpleVO.class);
        when(repository.getHotBooks(any(), anyInt()))
                .thenReturn(Single.just(ResultFactory.success(Collections.singletonList(b))));

        viewModel.loadHotBooks(null);

        assertEquals(LoadingState.CONTENT, viewModel.getLoadingState().getValue());
        assertEquals(1, viewModel.getHotBooks().getValue().size());
    }

    @Test
    public void loadHotBooks_emptyData_shouldEnterEmptyState() {
        when(repository.getHotBooks(any(), anyInt()))
                .thenReturn(Single.just(ResultFactory.success(Collections.emptyList())));

        viewModel.loadHotBooks(7L);

        assertEquals(LoadingState.EMPTY, viewModel.getLoadingState().getValue());
    }

    @Test
    public void loadHotBooks_apiFailure_shouldEnterEmptyState() {
        when(repository.getHotBooks(any(), anyInt()))
                .thenReturn(Single.just(ResultFactory.failure(500, "down")));

        viewModel.loadHotBooks(null);

        assertEquals(LoadingState.EMPTY, viewModel.getLoadingState().getValue());
    }

    @Test
    public void loadHotBooks_throwable_shouldPostError() {
        when(repository.getHotBooks(any(), anyInt()))
                .thenReturn(Single.error(new java.io.IOException("offline")));

        viewModel.loadHotBooks(null);

        assertNotNull(viewModel.getErrorEvent().getValue());
    }
}
