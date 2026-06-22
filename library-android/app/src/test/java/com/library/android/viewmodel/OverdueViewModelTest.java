package com.library.android.viewmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.library.android.model.BorrowRecordVO;
import com.library.android.repository.BorrowRepository;
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

public class OverdueViewModelTest {

    @Rule public InstantTaskExecutorRule instantTaskRule = new InstantTaskExecutorRule();
    @Rule public RxJava3SchedulerRule rxRule = new RxJava3SchedulerRule();

    private BorrowRepository repository;
    private OverdueViewModel viewModel;

    @Before
    public void setUp() {
        repository = mock(BorrowRepository.class);
        viewModel = new OverdueViewModel(repository);
    }

    @Test
    public void loadFirstPage_success_shouldEnterContentStateAndExposeRecords() {
        BorrowRecordVO r = mock(BorrowRecordVO.class);
        when(repository.getOverdueRecords(anyInt(), anyInt()))
                .thenReturn(Single.just(ResultFactory.success(
                        PageResults.of(Arrays.asList(r, r), 1, 1))));

        viewModel.loadFirstPage();

        assertEquals(LoadingState.CONTENT, viewModel.getLoadingState().getValue());
        assertEquals(2, viewModel.getOverdueList().getValue().size());
        assertFalse(viewModel.hasMore());  // 单页：无更多
    }

    @Test
    public void loadFirstPage_emptyData_shouldEnterEmptyState() {
        when(repository.getOverdueRecords(anyInt(), anyInt()))
                .thenReturn(Single.just(ResultFactory.success(
                        PageResults.of(Collections.emptyList(), 1, 1))));

        viewModel.loadFirstPage();

        assertEquals(LoadingState.EMPTY, viewModel.getLoadingState().getValue());
    }

    @Test
    public void loadFirstPage_apiFailure_shouldEnterErrorAndPostError() {
        when(repository.getOverdueRecords(anyInt(), anyInt()))
                .thenReturn(Single.just(ResultFactory.failure(500, "boom")));

        viewModel.loadFirstPage();

        assertEquals(LoadingState.ERROR, viewModel.getLoadingState().getValue());
        assertNotNull(viewModel.getErrorEvent().getValue());
    }

    @Test
    public void loadNextPage_shouldAppendNotReplace() {
        BorrowRecordVO first = mock(BorrowRecordVO.class);
        BorrowRecordVO second = mock(BorrowRecordVO.class);

        // 第一页（共 2 页）
        when(repository.getOverdueRecords(anyInt(), anyInt()))
                .thenReturn(Single.just(ResultFactory.success(
                        PageResults.of(Collections.singletonList(first), 1, 2))));
        viewModel.loadFirstPage();
        assertEquals(1, viewModel.getOverdueList().getValue().size());
        assertTrue(viewModel.hasMore());

        // 第二页 — 追加 second
        when(repository.getOverdueRecords(anyInt(), anyInt()))
                .thenReturn(Single.just(ResultFactory.success(
                        PageResults.of(Collections.singletonList(second), 2, 2))));
        viewModel.loadNextPage();

        assertEquals(2, viewModel.getOverdueList().getValue().size());
        assertFalse(viewModel.hasMore());
    }

    @Test
    public void loadNextPage_whenNoMorePages_shouldNotCallRepository() {
        // 单页加载完后 hasMore=false，再调 loadNextPage 应短路
        when(repository.getOverdueRecords(anyInt(), anyInt()))
                .thenReturn(Single.just(ResultFactory.success(
                        PageResults.of(Collections.emptyList(), 1, 1))));
        viewModel.loadFirstPage();

        viewModel.loadNextPage();
        // 仅第一次 loadFirstPage 调用 repo
        verify(repository, times(1)).getOverdueRecords(anyInt(), anyInt());
    }
}
