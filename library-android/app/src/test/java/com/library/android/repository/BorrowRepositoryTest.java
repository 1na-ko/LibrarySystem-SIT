package com.library.android.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.library.android.model.BorrowRecordVO;
import com.library.android.model.BorrowResultVO;
import com.library.android.model.PageResult;
import com.library.android.model.RenewResultVO;
import com.library.android.model.Result;
import com.library.android.network.exception.BizConflictException;
import com.library.android.testutil.AbstractRepositoryTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.reactivex.rxjava3.observers.TestObserver;

public class BorrowRepositoryTest extends AbstractRepositoryTest {

    private BorrowRepository repository;

    @Before
    public void setUp() throws Exception {
        startServer();
        repository = new BorrowRepository(api());
    }

    @After
    public void tearDown() throws Exception {
        shutdownServer();
    }

    @Test
    public void borrowBook_200_shouldReturnBorrowResult() {
        enqueueJson(200, successData("{\"borrowId\":100,\"bookTitle\":\"测试\",\"dueDate\":\"2026-07-30\",\"status\":\"BORROWED\"}"));

        TestObserver<Result<BorrowResultVO>> obs = repository.borrowBook(1L).test();
        obs.awaitDone(2, java.util.concurrent.TimeUnit.SECONDS);
        obs.assertValue(r -> {
            assertTrue(r.isSuccess());
            assertEquals(100L, r.getData().getBorrowId());
            return true;
        });
    }

    @Test
    public void borrowBook_409_shouldEmitBizConflictException() {
        enqueueJson(409, "{\"code\":409,\"message\":\"您已借阅该书\",\"data\":null}");

        TestObserver<Result<BorrowResultVO>> obs = repository.borrowBook(1L).test();
        obs.awaitDone(2, java.util.concurrent.TimeUnit.SECONDS);
        obs.assertError(BizConflictException.class);
        obs.assertError(t -> {
            BizConflictException e = (BizConflictException) t;
            return "您已借阅该书".equals(e.getServerMessage());
        });
    }

    @Test
    public void renewBook_200_shouldReturnRenewResult() {
        enqueueJson(200, successData("{\"borrowId\":1,\"newDueDate\":\"2026-07-30\","
                + "\"renewCount\":1,\"maxRenewReached\":false}"));

        TestObserver<Result<RenewResultVO>> obs = repository.renewBook(1L).test();
        obs.awaitDone(2, java.util.concurrent.TimeUnit.SECONDS);
        obs.assertValue(r -> {
            assertEquals("2026-07-30", r.getData().getNewDueDate());
            return true;
        });
    }

    @Test
    public void getMyBorrows_200_shouldReturnPaged() {
        enqueueJson(200, successData("{\"records\":[],\"total\":0,\"pageNum\":1,\"pageSize\":20,\"totalPages\":0}"));

        TestObserver<Result<PageResult<BorrowRecordVO>>> obs =
                repository.getMyBorrows(null, 1, 20).test();
        obs.awaitDone(2, java.util.concurrent.TimeUnit.SECONDS);
        obs.assertValue(r -> r.isSuccess() && r.getData().getRecords().isEmpty());
    }
}
