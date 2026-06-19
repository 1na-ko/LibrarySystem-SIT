package com.library.android.di;

import android.content.Context;

import com.google.gson.GsonBuilder;
import com.library.android.BuildConfig;
import com.library.android.network.AuthInterceptor;
import com.library.android.network.LibraryApi;
import com.library.android.network.SessionManager;
import com.library.android.network.TokenAuthenticator;
import com.library.android.network.MockInterceptor;
import com.library.android.network.Utf8FixTypeAdapterFactory;

import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * 网络层依赖提供模块.
 *
 * <p>提供全局单例：OkHttpClient、Retrofit、AuthApiService.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Module
@InstallIn(SingletonComponent.class)
public class NetworkModule {

    /**
     * 全局超时秒数.
     *
     * <p>调大到 30s：推荐端点 /users/me/recommendations 含 LLM 生成推荐理由，
     * 真机网络（移动数据/WiFi 到阿里云）RTT 较高，6-15s 的 LLM 耗时在真机上易超 15s
     * 触发 SocketTimeoutException 致推荐页空白（课设验证实测）.
     * KG 端点虽不含 LLM，但 Neo4j 多跳查询在冷启动时也可能偏慢，统一放宽.
     */
    private static final int TIMEOUT_SECONDS = 30;
    /** 打破 TokenAuthenticator ↔ Retrofit 循环依赖的持有者. */
    private static volatile LibraryApi sLibraryApi;

    @Provides
    @Singleton
    static TokenAuthenticator provideTokenAuthenticator(@ApplicationContext Context context,
                                                        SessionManager sessionManager) {
        return new TokenAuthenticator(context, () -> sLibraryApi, sessionManager);
    }

    @Provides
    @Singleton
    static OkHttpClient provideOkHttpClient(@ApplicationContext Context context,
                                            TokenAuthenticator tokenAuthenticator) {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        // 注：BODY level 会一次性读取整个响应 body（在 @Streaming 之前的 OkHttp 层），
        // 对 SSE 流式响应（text/event-stream）会缓冲整个流导致阻塞/异常。
        // 改用 BASIC（请求/响应行 + 耗时，不读 body），保证 SSE 流式不被破坏，同时保留基本调试信息.
        logging.setLevel(BuildConfig.DEBUG
                ? HttpLoggingInterceptor.Level.BASIC
                : HttpLoggingInterceptor.Level.NONE);

        MockInterceptor mockInterceptor = new MockInterceptor();
        mockInterceptor.setEnabled(BuildConfig.MOCK_ENABLED);

        return new OkHttpClient.Builder()
                .addInterceptor(mockInterceptor)          // 模拟拦截（最优先）
                .addInterceptor(new AuthInterceptor(context)) // 认证拦截
                .addInterceptor(logging)                  // 日志拦截
                .authenticator(tokenAuthenticator)        // 401 自动刷新 Token
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();
    }

    @Provides
    @Singleton
    static Retrofit provideRetrofit(OkHttpClient client) {
        // 通过 TypeAdapterFactory 修复 UTF-8 双重编码乱码（不干扰 Gson 类型系统）
        com.google.gson.Gson gson = new GsonBuilder()
                .registerTypeAdapterFactory(new Utf8FixTypeAdapterFactory())
                .create();
        return new Retrofit.Builder()
                .baseUrl(BuildConfig.BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .build();
    }

    @Provides
    @Singleton
    static LibraryApi provideLibraryApi(Retrofit retrofit) {
        LibraryApi api = retrofit.create(LibraryApi.class);
        sLibraryApi = api;  // 注入到 TokenAuthenticator 的 LibraryApiProvider
        return api;
    }
}