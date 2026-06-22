package com.library.android.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.library.android.model.DuplicateCheckResult;
import com.library.android.model.ElectronicResourceVO;
import com.library.android.model.GapAnalysisResult;
import com.library.android.model.NegotiationVO;
import com.library.android.model.PurchasePredictionVO;
import com.library.android.model.Result;
import com.library.android.model.SupplierVO;
import com.library.android.testutil.AbstractRepositoryTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import io.reactivex.rxjava3.observers.TestObserver;

public class AcquisitionRepositoryTest extends AbstractRepositoryTest {

    private AcquisitionRepository repository;

    @Before
    public void setUp() throws Exception {
        startServer();
        repository = new AcquisitionRepository(api());
    }

    @After
    public void tearDown() throws Exception {
        shutdownServer();
    }

    @Test
    public void predictDemand_200_shouldReturnPredictions() {
        enqueueJson(200, successData("[{\"subjectId\":1,\"subjectName\":\"计算机\","
                + "\"month\":\"2026-07\",\"predictedDemand\":12,\"confidence\":0.85}]"));

        TestObserver<Result<List<PurchasePredictionVO>>> obs =
                repository.predictDemand(1L, 3).test();
        obs.awaitDone(2, java.util.concurrent.TimeUnit.SECONDS);
        obs.assertValue(r -> {
            assertEquals(1, r.getData().size());
            assertEquals("2026-07", r.getData().get(0).getMonth());
            return true;
        });
    }

    @Test
    public void checkDuplicate_200_shouldReturnDuplicateResult() {
        enqueueJson(200, successData("{\"isDuplicate\":true,\"matches\":[]}"));

        TestObserver<Result<DuplicateCheckResult>> obs =
                repository.checkDuplicate("9787-x", null, null).test();
        obs.awaitDone(2, java.util.concurrent.TimeUnit.SECONDS);
        obs.assertValue(r -> r.isSuccess());
    }

    @Test
    public void analyzeGap_200_shouldReturnGapResult() {
        enqueueJson(200, successData("{\"subjectId\":1,\"items\":[],\"summary\":null}"));

        TestObserver<Result<GapAnalysisResult>> obs = repository.analyzeGap(1L).test();
        obs.awaitDone(2, java.util.concurrent.TimeUnit.SECONDS);
        obs.assertValue(r -> r.isSuccess());
    }

    @Test
    public void createNegotiation_200_shouldReturnNegotiationVO() {
        enqueueJson(200, successData("{\"id\":10,\"resourceId\":1,\"supplierId\":2,"
                + "\"status\":\"DRAFT\"}"));

        TestObserver<Result<NegotiationVO>> obs =
                repository.createNegotiation(1L, 2L).test();
        obs.awaitDone(2, java.util.concurrent.TimeUnit.SECONDS);
        obs.assertValue(r -> {
            assertNotNull(r.getData());
            assertEquals(10L, r.getData().getId());
            return true;
        });
    }

    @Test
    public void listSuppliers_200_shouldReturnSupplierList() {
        enqueueJson(200, successData("[{\"id\":1,\"name\":\"测试供应商\",\"contactInfo\":\"x\"}]"));

        TestObserver<Result<List<SupplierVO>>> obs = repository.listSuppliers().test();
        obs.awaitDone(2, java.util.concurrent.TimeUnit.SECONDS);
        obs.assertValue(r -> r.getData().size() == 1);
    }

    @Test
    public void listResources_200_shouldReturnResourceList() {
        enqueueJson(200, successData("[]"));

        TestObserver<Result<List<ElectronicResourceVO>>> obs = repository.listResources().test();
        obs.awaitDone(2, java.util.concurrent.TimeUnit.SECONDS);
        obs.assertValue(r -> r.isSuccess());
    }
}
