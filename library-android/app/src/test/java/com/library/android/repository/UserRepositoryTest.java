package com.library.android.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.library.android.model.BorrowRecordVO;
import com.library.android.model.BorrowStatsVO;
import com.library.android.model.PageResult;
import com.library.android.model.Result;
import com.library.android.model.UserProfile;
import com.library.android.testutil.AbstractRepositoryTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import io.reactivex.rxjava3.observers.TestObserver;

public class UserRepositoryTest extends AbstractRepositoryTest {

    private UserRepository repository;

    @Before
    public void setUp() throws Exception {
        startServer();
        repository = new UserRepository(api());
    }

    @After
    public void tearDown() throws Exception {
        shutdownServer();
    }

    @Test
    public void getMyProfile_200_shouldReturnProfile() {
        enqueueJson(200, successData("{\"id\":1,\"username\":\"alice\",\"realName\":\"Alice\","
                + "\"role\":\"STUDENT\",\"email\":\"a@x.com\"}"));

        TestObserver<Result<UserProfile>> obs = repository.getMyProfile().test();
        obs.awaitDone(2, java.util.concurrent.TimeUnit.SECONDS);
        obs.assertValue(r -> "alice".equals(r.getData().getUsername()));
    }

    @Test
    public void getMyStats_200_shouldReturnStats() {
        enqueueJson(200, successData("{\"totalBorrows\":24,\"currentBorrows\":3,"
                + "\"totalOverdue\":1,\"totalFines\":5.5,"
                + "\"categoryDistribution\":[],\"monthlyTrend\":[]}"));

        TestObserver<Result<BorrowStatsVO>> obs = repository.getMyStats().test();
        obs.awaitDone(2, java.util.concurrent.TimeUnit.SECONDS);
        obs.assertValue(r -> {
            assertEquals(24, r.getData().getTotalBorrows());
            assertEquals(3, r.getData().getCurrentBorrows());
            return true;
        });
    }

    @Test
    public void getMyHistory_200_shouldReturnPaged() {
        enqueueJson(200, successData("{\"records\":[],\"total\":0,\"pageNum\":1,\"pageSize\":20,\"totalPages\":0}"));

        TestObserver<Result<PageResult<BorrowRecordVO>>> obs =
                repository.getMyHistory(null, 1, 20).test();
        obs.awaitDone(2, java.util.concurrent.TimeUnit.SECONDS);
        obs.assertValue(r -> r.getData().getRecords().isEmpty());
    }

    @Test
    public void updateMyProfile_200_shouldSucceed() {
        enqueueJson(200, successNull());

        TestObserver<Result<Void>> obs =
                repository.updateMyProfile(Collections.singletonMap("email", "x@y.com")).test();
        obs.awaitDone(2, java.util.concurrent.TimeUnit.SECONDS);
        obs.assertValue(r -> r.isSuccess());
    }

    @Test
    public void getRecommendations_200_shouldReturnList() {
        enqueueJson(200, successData("[]"));

        TestObserver<Result<java.util.List<com.library.android.model.BookRecommendVO>>> obs =
                repository.getRecommendations(20).test();
        obs.awaitDone(2, java.util.concurrent.TimeUnit.SECONDS);
        obs.assertValue(r -> {
            assertNotNull(r.getData());
            return true;
        });
    }
}
