package com.library.android.viewmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.library.android.model.CategoryVO;
import com.library.android.repository.BookRepository;
import com.library.android.testutil.ResultFactory;
import com.library.android.testutil.RxJava3SchedulerRule;
import com.library.android.ui.common.LoadingState;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import io.reactivex.rxjava3.core.Single;

public class CategoryTreeViewModelTest {

    @Rule public InstantTaskExecutorRule instantTaskRule = new InstantTaskExecutorRule();
    @Rule public RxJava3SchedulerRule rxRule = new RxJava3SchedulerRule();

    private BookRepository repository;
    private CategoryTreeViewModel viewModel;

    @Before
    public void setUp() {
        repository = mock(BookRepository.class);
        viewModel = new CategoryTreeViewModel(repository);
    }

    @Test
    public void loadCategories_success_shouldEnterContentState() {
        CategoryVO c = mock(CategoryVO.class);
        when(repository.getCategoryTree())
                .thenReturn(Single.just(ResultFactory.success(Arrays.asList(c, c))));

        viewModel.loadCategories();

        assertEquals(LoadingState.CONTENT, viewModel.getLoadingState().getValue());
        assertNotNull(viewModel.getCategories().getValue());
        assertEquals(2, viewModel.getCategories().getValue().size());
    }

    @Test
    public void loadCategories_emptyResponse_shouldEnterEmptyState() {
        when(repository.getCategoryTree())
                .thenReturn(Single.just(ResultFactory.success(Collections.emptyList())));

        viewModel.loadCategories();

        assertEquals(LoadingState.EMPTY, viewModel.getLoadingState().getValue());
    }

    @Test
    public void loadCategories_apiFailure_shouldEnterErrorStateAndPostError() {
        when(repository.getCategoryTree())
                .thenReturn(Single.just(ResultFactory.failure(500, "boom")));

        viewModel.loadCategories();

        assertEquals(LoadingState.ERROR, viewModel.getLoadingState().getValue());
        assertNotNull(viewModel.getErrorEvent().getValue());
        assertNull(viewModel.getCategories().getValue());
    }

    @Test
    public void loadCategories_networkException_shouldEnterErrorStateAndPostError() {
        when(repository.getCategoryTree())
                .thenReturn(Single.error(new java.io.IOException("offline")));

        viewModel.loadCategories();

        assertEquals(LoadingState.ERROR, viewModel.getLoadingState().getValue());
        assertNotNull(viewModel.getErrorEvent().getValue());
    }
}
