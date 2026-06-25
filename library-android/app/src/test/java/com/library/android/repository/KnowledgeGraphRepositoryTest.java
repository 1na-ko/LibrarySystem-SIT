package com.library.android.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.library.android.model.EntitySearchResult;
import com.library.android.model.KnowledgeGraphVO;
import com.library.android.model.Result;
import com.library.android.model.TraceGraph;
import com.library.android.testutil.AbstractRepositoryTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import io.reactivex.rxjava3.observers.TestObserver;

public class KnowledgeGraphRepositoryTest extends AbstractRepositoryTest {

    private KnowledgeGraphRepository repository;

    @Before
    public void setUp() throws Exception {
        startServer();
        repository = new KnowledgeGraphRepository(api());
    }

    @After
    public void tearDown() throws Exception {
        shutdownServer();
    }

    @Test
    public void getBookGraph_200_shouldReturnGraph() {
        enqueueJson(200, successData("{\"nodes\":[{\"id\":1,\"label\":\"图书\",\"type\":\"BOOK\"}],"
                + "\"edges\":[]}"));

        TestObserver<Result<KnowledgeGraphVO>> obs = repository.getBookGraph(1L, 2).test();
        obs.awaitDone(2, java.util.concurrent.TimeUnit.SECONDS);
        obs.assertValue(r -> {
            assertEquals(1, r.getData().getNodes().size());
            return true;
        });
    }

    @Test
    public void traceLiterature_200_shouldReturnTraceGraph() {
        enqueueJson(200, successData("{\"sourceBook\":{\"id\":1,\"title\":\"x\"},\"paths\":[]}"));

        TestObserver<Result<TraceGraph>> obs =
                repository.traceLiterature(1L, "BOTH", 3).test();
        obs.awaitDone(2, java.util.concurrent.TimeUnit.SECONDS);
        obs.assertValue(r -> r.isSuccess());
    }

    @Test
    public void searchEntities_200_shouldFlattenNodesToEntityResults() {
        // 后端返回 KnowledgeGraphVO，Repository 转换为 List<EntitySearchResult>
        enqueueJson(200, successData("{\"nodes\":["
                + "{\"id\":1,\"label\":\"算法\",\"type\":\"KEYWORD\","
                + "\"properties\":{\"pagerank\":0.91}}],"
                + "\"edges\":[]}"));

        TestObserver<Result<List<EntitySearchResult>>> obs =
                repository.searchEntities("算法", "KEYWORD").test();
        obs.awaitDone(2, java.util.concurrent.TimeUnit.SECONDS);
        obs.assertValue(r -> {
            assertEquals(1, r.getData().size());
            EntitySearchResult e = r.getData().get(0);
            assertEquals("算法", e.getEntityName());
            assertEquals("KEYWORD", e.getEntityType());
            assertTrue(e.getPagerank() > 0.9);
            return true;
        });
    }

    @Test
    public void getKeyPath_200_shouldReturnTrace() {
        enqueueJson(200, successData("{\"sourceBook\":{\"id\":1,\"title\":\"a\"},\"paths\":[]}"));

        TestObserver<Result<TraceGraph>> obs = repository.getKeyPath(1L, 2L).test();
        obs.awaitDone(2, java.util.concurrent.TimeUnit.SECONDS);
        obs.assertNoErrors();
    }

    @Test
    public void searchEntities_empty_shouldReturnEmptyList() {
        enqueueJson(200, successData("{\"nodes\":[],\"edges\":[]}"));

        TestObserver<Result<List<EntitySearchResult>>> obs =
                repository.searchEntities("nonexistent", "BOOK").test();
        obs.awaitDone(2, java.util.concurrent.TimeUnit.SECONDS);
        obs.assertValue(r -> {
            assertTrue(r.isSuccess());
            assertTrue(r.getData().isEmpty());
            return true;
        });
    }
}
