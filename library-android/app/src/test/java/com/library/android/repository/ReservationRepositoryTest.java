package com.library.android.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.library.android.model.PageResult;
import com.library.android.model.ReservationVO;
import com.library.android.model.Result;
import com.library.android.network.exception.BizConflictException;
import com.library.android.network.exception.PermissionDeniedException;
import com.library.android.testutil.AbstractRepositoryTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.reactivex.rxjava3.observers.TestObserver;

public class ReservationRepositoryTest extends AbstractRepositoryTest {

    private ReservationRepository repository;

    @Before
    public void setUp() throws Exception {
        startServer();
        repository = new ReservationRepository(api());
    }

    @After
    public void tearDown() throws Exception {
        shutdownServer();
    }

    @Test
    public void reserveBook_200_shouldReturnReservation() {
        enqueueJson(200, successData("{\"id\":10,\"reserveTime\":\"2026-06-19\","
                + "\"queuePosition\":4,\"status\":\"WAITING\","
                + "\"book\":{\"id\":2,\"title\":\"测试\",\"author\":\"a\",\"availCopies\":0}}"));

        TestObserver<Result<ReservationVO>> obs = repository.reserveBook(2L).test();
        obs.awaitDone(2, java.util.concurrent.TimeUnit.SECONDS);
        obs.assertValue(r -> {
            assertEquals(4, r.getData().getQueuePosition());
            return true;
        });
    }

    @Test
    public void cancelReservation_409_shouldEmitBizConflict() {
        enqueueJson(409, "{\"code\":409,\"message\":\"已取消\",\"data\":null}");

        TestObserver<Result<Void>> obs = repository.cancelReservation(1L).test();
        obs.awaitDone(2, java.util.concurrent.TimeUnit.SECONDS);
        obs.assertError(BizConflictException.class);
    }

    @Test
    public void getQueuePosition_200_shouldReturnInteger() {
        enqueueJson(200, successData("3"));

        TestObserver<Result<Integer>> obs = repository.getQueuePosition(1L).test();
        obs.awaitDone(2, java.util.concurrent.TimeUnit.SECONDS);
        obs.assertValue(r -> r.getData() == 3);
    }

    @Test
    public void getMyReservations_403_shouldEmitPermissionDenied() {
        enqueueJson(403, "{\"code\":403,\"message\":\"无权限\",\"data\":null}");

        TestObserver<Result<PageResult<ReservationVO>>> obs =
                repository.getMyReservations(null, 1, 20).test();
        obs.awaitDone(2, java.util.concurrent.TimeUnit.SECONDS);
        obs.assertError(PermissionDeniedException.class);
    }

    @Test
    public void getMyReservations_empty_shouldReturnEmptyRecords() {
        enqueueJson(200, successData("{\"records\":[],\"total\":0,\"pageNum\":1,\"pageSize\":20,\"totalPages\":0}"));

        TestObserver<Result<PageResult<ReservationVO>>> obs =
                repository.getMyReservations(null, 1, 20).test();
        obs.awaitDone(2, java.util.concurrent.TimeUnit.SECONDS);
        obs.assertValue(r -> {
            assertTrue(r.isSuccess());
            assertTrue(r.getData().getRecords().isEmpty());
            return true;
        });
    }
}
