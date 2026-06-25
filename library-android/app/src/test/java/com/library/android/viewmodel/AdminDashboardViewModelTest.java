package com.library.android.viewmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.library.android.model.DashboardVO;
import com.library.android.repository.AdminRepository;
import com.library.android.testutil.ResultFactory;
import com.library.android.testutil.RxJava3SchedulerRule;
import com.library.android.ui.common.LoadingState;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import io.reactivex.rxjava3.core.Single;

public class AdminDashboardViewModelTest {

    @Rule public InstantTaskExecutorRule instantTaskRule = new InstantTaskExecutorRule();
    @Rule public RxJava3SchedulerRule rxRule = new RxJava3SchedulerRule();

    private AdminRepository repository;
    private AdminDashboardViewModel viewModel;

    @Before
    public void setUp() {
        repository = mock(AdminRepository.class);
        viewModel = new AdminDashboardViewModel(repository);
    }

    @Test
    public void load_success_shouldExposeDashboardAndContentState() {
        DashboardVO d = mock(DashboardVO.class);
        when(repository.getDashboard())
                .thenReturn(Single.just(ResultFactory.success(d)));

        viewModel.load();

        assertNotNull(viewModel.getDashboard().getValue());
        assertEquals(LoadingState.CONTENT, viewModel.getLoadingState().getValue());
    }

    @Test
    public void load_apiFailure_shouldEnterErrorState() {
        when(repository.getDashboard())
                .thenReturn(Single.just(ResultFactory.failure(500, "boom")));

        viewModel.load();

        assertEquals(LoadingState.ERROR, viewModel.getLoadingState().getValue());
    }

    @Test
    public void load_throwable_shouldPostErrorAndEnterErrorState() {
        when(repository.getDashboard())
                .thenReturn(Single.error(new java.io.IOException("offline")));

        viewModel.load();

        assertEquals(LoadingState.ERROR, viewModel.getLoadingState().getValue());
        assertNotNull(viewModel.getErrorEvent().getValue());
    }

    @Test
    public void rebuildKnowledgeGraph_success_shouldExposeProcessedCountAndClearProgress() {
        when(repository.rebuildKgAll())
                .thenReturn(Single.just(ResultFactory.success(42)));

        viewModel.rebuildKnowledgeGraph();

        assertEquals(Integer.valueOf(42), viewModel.getRebuildResult().getValue());
        assertEquals(Boolean.FALSE, viewModel.isRebuildInProgress().getValue());
    }

    @Test
    public void rebuildKnowledgeGraph_failure_shouldPostErrorAndClearProgress() {
        when(repository.rebuildKgAll())
                .thenReturn(Single.just(ResultFactory.failure(500, "重建失败")));

        viewModel.rebuildKnowledgeGraph();

        assertNotNull(viewModel.getErrorEvent().getValue());
        assertEquals(Boolean.FALSE, viewModel.isRebuildInProgress().getValue());
        assertNull(viewModel.getRebuildResult().getValue());
    }

    @Test
    public void rebuildKnowledgeGraph_concurrentCalls_shouldDebounce() {
        when(repository.rebuildKgAll()).thenReturn(Single.never());

        viewModel.rebuildKnowledgeGraph();
        viewModel.rebuildKnowledgeGraph();

        verify(repository, times(1)).rebuildKgAll();
    }
}
