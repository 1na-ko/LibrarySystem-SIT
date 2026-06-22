package com.library.android.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.library.android.model.BookDetailVO;
import com.library.android.model.BookSimpleVO;
import com.library.android.model.CategoryVO;
import com.library.android.model.PageResult;
import com.library.android.model.Result;
import com.library.android.model.SuggestVO;
import com.library.android.network.exception.ServiceUnavailableException;
import com.library.android.testutil.AbstractRepositoryTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import io.reactivex.rxjava3.observers.TestObserver;

public class BookRepositoryTest extends AbstractRepositoryTest {

    private BookRepository repository;

    @Before
    public void setUp() throws Exception {
        startServer();
        repository = new BookRepository(api());
    }

    @After
    public void tearDown() throws Exception {
        shutdownServer();
    }

    @Test
    public void searchBooks_200_shouldReturnPagedResults() {
        enqueueJson(200, successData("{\"records\":[{\"id\":1,\"title\":\"测试图书\",\"author\":\"作者\","
                + "\"publisher\":\"出版社\",\"categoryName\":\"计算机\",\"availCopies\":3}],"
                + "\"total\":1,\"pageNum\":1,\"pageSize\":20,\"totalPages\":1}"));

        TestObserver<Result<PageResult<BookSimpleVO>>> obs =
                repository.searchBooks("test", null, null, null, 1, 20).test();
        obs.awaitDone(2, java.util.concurrent.TimeUnit.SECONDS);
        obs.assertValue(r -> {
            assertTrue(r.isSuccess());
            assertEquals(1, r.getData().getRecords().size());
            assertEquals("测试图书", r.getData().getRecords().get(0).getTitle());
            return true;
        });
    }

    @Test
    public void getBookDetail_200_shouldReturnDetail() {
        enqueueJson(200, successData("{\"id\":1,\"isbn\":\"9787-x\",\"title\":\"详情测试\","
                + "\"author\":\"测试作者\",\"availCopies\":2,\"totalCopies\":3,\"reservationCount\":1}"));

        TestObserver<Result<BookDetailVO>> obs = repository.getBookDetail(1L).test();
        obs.awaitDone(2, java.util.concurrent.TimeUnit.SECONDS);
        obs.assertValue(r -> {
            BookDetailVO d = r.getData();
            assertEquals("详情测试", d.getTitle());
            assertEquals(2, d.getAvailCopies());
            return true;
        });
    }

    @Test
    public void getCategoryTree_200_shouldReturnList() {
        enqueueJson(200, successData("[{\"id\":1,\"name\":\"计算机\",\"parentId\":null,\"sortOrder\":1}]"));

        TestObserver<Result<List<CategoryVO>>> obs = repository.getCategoryTree().test();
        obs.awaitDone(2, java.util.concurrent.TimeUnit.SECONDS);
        obs.assertValue(r -> {
            assertEquals(1, r.getData().size());
            assertEquals("计算机", r.getData().get(0).getName());
            return true;
        });
    }

    @Test
    public void getSuggestions_200_shouldReturnSuggestions() {
        enqueueJson(200, successData("[{\"text\":\"数据结构\",\"type\":\"BOOK\"}]"));

        TestObserver<Result<List<SuggestVO>>> obs =
                repository.getSuggestions("数据", 5).test();
        obs.awaitDone(2, java.util.concurrent.TimeUnit.SECONDS);
        obs.assertValue(r -> {
            assertNotNull(r.getData());
            assertEquals(1, r.getData().size());
            return true;
        });
    }

    @Test
    public void getHotBooks_503_shouldEmitServiceUnavailable() {
        enqueueJson(503, "{\"code\":503,\"message\":\"维护中\",\"data\":null}");

        TestObserver<Result<List<BookSimpleVO>>> obs = repository.getHotBooks(null, 10).test();
        obs.awaitDone(2, java.util.concurrent.TimeUnit.SECONDS);
        obs.assertError(ServiceUnavailableException.class);
    }

    @Test
    public void getHotBooks_empty_shouldReturnEmptyList() {
        enqueueJson(200, successData("[]"));

        TestObserver<Result<List<BookSimpleVO>>> obs = repository.getHotBooks(null, 10).test();
        obs.awaitDone(2, java.util.concurrent.TimeUnit.SECONDS);
        obs.assertValue(r -> {
            assertTrue(r.isSuccess());
            assertTrue(r.getData().isEmpty());
            return true;
        });
    }

    @Test
    public void getBookDetail_404_shouldThrowNotFoundException() {
        enqueueJson(404, "{\"code\":404,\"message\":\"not found\",\"data\":null}");

        TestObserver<Result<BookDetailVO>> obs = repository.getBookDetail(999L).test();
        obs.awaitDone(2, java.util.concurrent.TimeUnit.SECONDS);
        obs.assertError(com.library.android.network.exception.NotFoundException.class);
    }
}
