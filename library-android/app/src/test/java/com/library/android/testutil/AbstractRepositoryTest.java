package com.library.android.testutil;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.library.android.network.LibraryApi;
import com.library.android.network.Utf8FixTypeAdapterFactory;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Repository 测试基类 — 封装 MockWebServer + Retrofit 客户端构造.
 *
 * <p>使用方式：在 @Before 调 {@link #startServer()}, @After 调 {@link #shutdownServer()}.
 * 业务测试中通过 {@link #enqueueJson(int, String)} 入队 mock 响应，
 * 通过 {@link #api()} 得到指向 mock 服务器的 {@link LibraryApi} 实例.
 *
 * <p>所有 Repository 测试不依赖 Hilt — 直接 {@code new XxxRepository(api())} 即可.
 */
public abstract class AbstractRepositoryTest {

    protected MockWebServer server;
    private LibraryApi api;

    public void startServer() throws IOException {
        server = new MockWebServer();
        server.start();
        Gson gson = new GsonBuilder()
                .registerTypeAdapterFactory(new Utf8FixTypeAdapterFactory())
                .create();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(server.url("/api/v1/"))
                .client(new OkHttpClient.Builder().build())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .build();
        api = retrofit.create(LibraryApi.class);
    }

    public void shutdownServer() throws IOException {
        if (server != null) server.shutdown();
    }

    public LibraryApi api() {
        return api;
    }

    public void enqueueJson(int status, String json) {
        server.enqueue(new MockResponse()
                .setResponseCode(status)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .setBody(json));
    }

    public void enqueueEmpty(int status) {
        server.enqueue(new MockResponse().setResponseCode(status));
    }

    /** 构造 Result&lt;T&gt; JSON. */
    public static String resultJson(int code, String message, String dataJson) {
        return "{\"code\":" + code + ",\"message\":\"" + message + "\","
                + "\"data\":" + (dataJson == null ? "null" : dataJson)
                + ",\"timestamp\":1700000000000}";
    }

    public static String successData(String dataJson) {
        return resultJson(200, "OK", dataJson);
    }

    public static String successNull() {
        return resultJson(200, "OK", null);
    }
}
