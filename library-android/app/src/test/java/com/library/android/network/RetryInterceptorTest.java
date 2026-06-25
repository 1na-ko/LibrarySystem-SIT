package com.library.android.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;

/**
 * P2-07：RetryInterceptor 行为验证 — 仅幂等方法重试、IOException 重试、5xx 不重试.
 */
public class RetryInterceptorTest {

    private MockWebServer server;
    private OkHttpClient client;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        client = new OkHttpClient.Builder()
                .addInterceptor(new RetryInterceptor())
                .connectTimeout(2, TimeUnit.SECONDS)
                .readTimeout(2, TimeUnit.SECONDS)
                .build();
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void getRequest_ioException_shouldRetryOnceThenSucceed() throws IOException {
        // 第一次响应模拟 connection reset，第二次 200
        server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));
        server.enqueue(new MockResponse().setBody("ok"));

        Request req = new Request.Builder().url(server.url("/x")).get().build();
        try (Response resp = client.newCall(req).execute()) {
            assertEquals(200, resp.code());
            assertNotNull(resp.body());
            assertEquals("ok", resp.body().string());
        }
        assertEquals(2, server.getRequestCount());
    }

    @Test
    public void postRequest_ioException_shouldNotRetry() throws IOException {
        server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));

        Request req = new Request.Builder()
                .url(server.url("/x"))
                .post(okhttp3.RequestBody.create("{}", okhttp3.MediaType.parse("application/json")))
                .build();
        try (Response resp = client.newCall(req).execute()) {
            fail("POST 在 IOException 时不应重试也不应得到响应");
        } catch (IOException expected) {
            // expected
        }
        assertEquals(1, server.getRequestCount());  // 仅尝试一次
    }

    @Test
    public void getRequest_500Response_shouldNotRetry() throws IOException {
        // 5xx 不属于 IOException — 不应触发重试
        server.enqueue(new MockResponse().setResponseCode(500).setBody("err"));

        Request req = new Request.Builder().url(server.url("/x")).get().build();
        try (Response resp = client.newCall(req).execute()) {
            assertEquals(500, resp.code());
        }
        assertEquals(1, server.getRequestCount());
    }
}
