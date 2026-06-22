package com.library.android.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.library.android.model.BookCreateRequest;
import com.library.android.model.BookDetailVO;
import com.library.android.model.DashboardVO;
import com.library.android.model.PageResult;
import com.library.android.model.Result;
import com.library.android.model.UserManageVO;
import com.library.android.network.exception.PermissionDeniedException;
import com.library.android.testutil.AbstractRepositoryTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.reactivex.rxjava3.observers.TestObserver;

public class AdminRepositoryTest extends AbstractRepositoryTest {

    private AdminRepository repository;

    @Before
    public void setUp() throws Exception {
        startServer();
        repository = new AdminRepository(api());
    }

    @After
    public void tearDown() throws Exception {
        shutdownServer();
    }

    @Test
    public void listUsers_200_shouldReturnPagedUsers() {
        enqueueJson(200, successData("{\"records\":[{\"id\":1,\"username\":\"admin\","
                + "\"realName\":\"管理员\",\"role\":\"ADMIN\",\"status\":\"ACTIVE\","
                + "\"currentBorrows\":0,\"totalOverdue\":0}],"
                + "\"total\":1,\"pageNum\":1,\"pageSize\":20,\"totalPages\":1}"));

        TestObserver<Result<PageResult<UserManageVO>>> obs =
                repository.listUsers(null, null, null, 1, 20).test();
        obs.awaitDone(2, java.util.concurrent.TimeUnit.SECONDS);
        obs.assertValue(r -> {
            assertEquals(1, r.getData().getRecords().size());
            return true;
        });
    }

    @Test
    public void updateUserStatus_403_shouldEmitPermissionDenied() {
        enqueueJson(403, "{\"code\":403,\"message\":\"权限不足\",\"data\":null}");

        TestObserver<Result<Void>> obs = repository.updateUserStatus(1L, "FROZEN").test();
        obs.awaitDone(2, java.util.concurrent.TimeUnit.SECONDS);
        obs.assertError(PermissionDeniedException.class);
    }

    @Test
    public void getDashboard_200_shouldReturnVO() {
        enqueueJson(200, successData("{\"todayBorrows\":10,\"todayReturns\":5,"
                + "\"todayOverdue\":1,\"activeBorrowers\":3,"
                + "\"monthTrend\":[],\"hotCategories\":[]}"));

        TestObserver<Result<DashboardVO>> obs = repository.getDashboard().test();
        obs.awaitDone(2, java.util.concurrent.TimeUnit.SECONDS);
        obs.assertValue(r -> {
            assertEquals(10, r.getData().getTodayBorrows());
            return true;
        });
    }

    @Test
    public void createBook_200_shouldReturnDetailVO() {
        enqueueJson(200, successData("{\"id\":100,\"isbn\":\"9787-x\",\"title\":\"new\","
                + "\"author\":\"a\",\"availCopies\":5,\"totalCopies\":5}"));

        TestObserver<Result<BookDetailVO>> obs = repository.createBook(new BookCreateRequest()).test();
        obs.awaitDone(2, java.util.concurrent.TimeUnit.SECONDS);
        obs.assertValue(r -> {
            assertNotNull(r.getData());
            assertEquals("new", r.getData().getTitle());
            return true;
        });
    }

    @Test
    public void rebuildKgAll_200_shouldReturnProcessedCount() {
        enqueueJson(200, successData("123"));

        TestObserver<Result<Integer>> obs = repository.rebuildKgAll().test();
        obs.awaitDone(5, java.util.concurrent.TimeUnit.SECONDS);
        obs.assertValue(r -> r.getData() == 123);
    }
}
