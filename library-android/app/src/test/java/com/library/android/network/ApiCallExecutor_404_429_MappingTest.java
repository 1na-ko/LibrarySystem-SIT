package com.library.android.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.library.android.model.LoginResponse;
import com.library.android.model.Result;
import com.library.android.network.exception.NotFoundException;
import com.library.android.network.exception.RateLimitException;
import com.library.android.testutil.AbstractRepositoryTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * P2-07：ApiCallExecutor 异常映射扩展测试 — 404 / 429.
 *
 * <p>原 ApiCallExecutorTest 仅有间接覆盖（Repository 测试包含 401/403/409/503）；
 * 本测试针对新增的 NotFoundException / RateLimitException 做精准断言.
 */
public class ApiCallExecutor_404_429_MappingTest extends AbstractRepositoryTest {

    @Before
    public void setUp() throws Exception {
        startServer();
    }

    @After
    public void tearDown() throws Exception {
        shutdownServer();
    }

    @Test
    public void execute_404_shouldThrowNotFoundException() {
        enqueueJson(404, "{\"code\":404,\"message\":\"图书不存在\",\"data\":null}");

        try {
            ApiCallExecutor.execute(api().getBookDetail(999L));
            fail("应抛出 NotFoundException");
        } catch (NotFoundException e) {
            assertEquals(404, e.getHttpCode());
            assertEquals("图书不存在", e.getServerMessage());
        }
    }

    @Test
    public void execute_429_shouldThrowRateLimitExceptionWithRetryAfter() {
        // 同时携带 Retry-After 头
        server.enqueue(new okhttp3.mockwebserver.MockResponse()
                .setResponseCode(429)
                .addHeader("Retry-After", "30")
                .addHeader("Content-Type", "application/json")
                .setBody("{\"code\":429,\"message\":\"请求过于频繁\",\"data\":null}"));

        try {
            ApiCallExecutor.execute(api().login(new com.library.android.model.LoginRequest("u", "p")));
            fail("应抛出 RateLimitException");
        } catch (RateLimitException e) {
            assertEquals(429, e.getHttpCode());
            assertEquals(30, e.getRetryAfterSeconds());
            assertEquals("请求过于频繁", e.getServerMessage());
        }
    }

    @Test
    public void execute_429_withoutRetryAfter_shouldDefaultToZero() {
        enqueueJson(429, "{\"code\":429,\"message\":\"limit\",\"data\":null}");

        try {
            ApiCallExecutor.execute(api().login(new com.library.android.model.LoginRequest("u", "p")));
            fail("应抛出 RateLimitException");
        } catch (RateLimitException e) {
            assertEquals(0, e.getRetryAfterSeconds());
        }
    }

    @Test
    public void execute_429_invalidRetryAfter_shouldFallbackToZero() {
        server.enqueue(new okhttp3.mockwebserver.MockResponse()
                .setResponseCode(429)
                .addHeader("Retry-After", "Wed, 21 Oct 2026 07:28:00 GMT")  // HTTP date 格式（不是秒数）
                .addHeader("Content-Type", "application/json")
                .setBody("{\"code\":429,\"message\":\"x\",\"data\":null}"));

        try {
            ApiCallExecutor.execute(api().login(new com.library.android.model.LoginRequest("u", "p")));
            fail();
        } catch (RateLimitException e) {
            // 当前实现不支持 HTTP date 格式，降级为 0（合理保守）
            assertEquals(0, e.getRetryAfterSeconds());
        }
    }
}
