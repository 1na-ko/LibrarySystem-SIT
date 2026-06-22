package com.library.android.viewmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.library.android.model.EntitySearchResult;
import com.library.android.model.KnowledgeGraphVO;
import com.library.android.model.TraceGraph;
import com.library.android.repository.KnowledgeGraphRepository;
import com.library.android.testutil.ResultFactory;
import com.library.android.testutil.RxJava3SchedulerRule;
import com.library.android.ui.common.LoadingState;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;

import io.reactivex.rxjava3.core.Single;

public class KnowledgeGraphViewModelTest {

    @Rule public InstantTaskExecutorRule instantTaskRule = new InstantTaskExecutorRule();
    @Rule public RxJava3SchedulerRule rxRule = new RxJava3SchedulerRule();

    private KnowledgeGraphRepository repository;
    private KnowledgeGraphViewModel viewModel;

    @Before
    public void setUp() {
        repository = mock(KnowledgeGraphRepository.class);
        viewModel = new KnowledgeGraphViewModel(repository);
    }

    @Test
    public void loadBookGraph_success_shouldExposeGraphAndContentState() {
        KnowledgeGraphVO graph = mock(KnowledgeGraphVO.class);
        when(repository.getBookGraph(anyLong(), anyInt()))
                .thenReturn(Single.just(ResultFactory.success(graph)));

        viewModel.loadBookGraph(1L, 2);

        assertNotNull(viewModel.getBookGraph().getValue());
        assertEquals(LoadingState.CONTENT, viewModel.getLoadingState().getValue());
    }

    @Test
    public void loadBookGraph_failure_shouldPostError() {
        when(repository.getBookGraph(anyLong(), anyInt()))
                .thenReturn(Single.just(ResultFactory.failure(404, "not found")));

        viewModel.loadBookGraph(1L, 2);

        assertNotNull(viewModel.getErrorEvent().getValue());
        assertNull(viewModel.getBookGraph().getValue());
    }

    @Test
    public void traceLiterature_success_shouldExposeTraceGraph() {
        TraceGraph trace = mock(TraceGraph.class);
        when(repository.traceLiterature(anyLong(), anyString(), anyInt()))
                .thenReturn(Single.just(ResultFactory.success(trace)));

        viewModel.traceLiterature(1L, "BOTH", 3);

        assertNotNull(viewModel.getTraceGraph().getValue());
    }

    @Test
    public void loadSubjectNetwork_success_shouldExposeSubjectGraph() {
        KnowledgeGraphVO graph = mock(KnowledgeGraphVO.class);
        when(repository.getSubjectNetwork(anyString(), anyInt()))
                .thenReturn(Single.just(ResultFactory.success(graph)));

        viewModel.loadSubjectNetwork("机器学习", 5);

        assertNotNull(viewModel.getSubjectNetwork().getValue());
    }

    @Test
    public void searchEntities_success_shouldExposeResults() {
        EntitySearchResult e = mock(EntitySearchResult.class);
        when(repository.searchEntities(anyString(), anyString()))
                .thenReturn(Single.just(ResultFactory.success(Collections.singletonList(e))));

        viewModel.searchEntities("数据", "KEYWORD");

        assertNotNull(viewModel.getEntityResults().getValue());
        assertEquals(1, viewModel.getEntityResults().getValue().size());
    }

    @Test
    public void loadKeyPath_success_shouldExposeKeyPath() {
        TraceGraph path = mock(TraceGraph.class);
        when(repository.getKeyPath(anyLong(), anyLong()))
                .thenReturn(Single.just(ResultFactory.success(path)));

        viewModel.loadKeyPath(1L, 2L);

        assertNotNull(viewModel.getKeyPath().getValue());
    }

    @Test
    public void loadKeyPath_failure_shouldPostError() {
        when(repository.getKeyPath(anyLong(), anyLong()))
                .thenReturn(Single.error(new RuntimeException("offline")));

        viewModel.loadKeyPath(1L, 2L);

        assertNotNull(viewModel.getErrorEvent().getValue());
    }
}
