package com.library.android.di;

import android.content.Context;
import android.util.Log;

import com.google.gson.GsonBuilder;
import com.library.android.BuildConfig;
import com.library.android.network.AuthInterceptor;
import com.library.android.network.CertificatePinnerProvider;
import com.library.android.network.LibraryApi;
import com.library.android.network.MockInterceptor;
import com.library.android.network.RetryInterceptor;
import com.library.android.network.SessionManager;
import com.library.android.network.TokenAuthenticator;
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
 * <p>提供全局单例：OkHttpClient、Retrofit、LibraryApi.
 *
 * <p>P0-02 / P0-05 协议与拦截器隔离要点（2026-06-19）：
 * <ul>
 *   <li>BASE_URL / USE_HTTPS 由 build.gradle.kts 驱动，构造 Retrofit 时记录协议日志</li>
 *   <li>MockInterceptor 仅在 {@code BuildConfig.DEBUG && BuildConfig.MOCK_ENABLED} 同时为 true 时
 *       才被注册到 OkHttpClient；Release 构建中该拦截器永不进入责任链，
 *       配合 R8 minify 后类本身将被裁剪，杜绝反射启用风险</li>
 *   <li>Release 构建若 BASE_URL 为 http:// 会被 verifyReleaseHttps Gradle task 阻断</li>
 * </ul>
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Module
@InstallIn(SingletonComponent.class)
public class NetworkModule {

    private static final String TAG = "NetworkModule";

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

        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        // ─────────────────────────────────────────────────────────────────────
        // P0-05: MockInterceptor 仅在 Debug 构建 + MOCK_ENABLED 同时为 true 时启用
        //        Release 构建跳过整段，配合 R8 minify 类本身将被裁剪
        // ─────────────────────────────────────────────────────────────────────
        if (BuildConfig.DEBUG && BuildConfig.MOCK_ENABLED) {
            MockInterceptor mockInterceptor = new MockInterceptor();
            mockInterceptor.setEnabled(true);
            builder.addInterceptor(mockInterceptor);
            Log.i(TAG, "MockInterceptor 已启用（Debug + MOCK_ENABLED=true）");
        }

        return builder
                .addInterceptor(new RetryInterceptor())       // P2-07：幂等 GET / HEAD 网络重试
                .addInterceptor(new AuthInterceptor(context)) // 认证拦截
                .addInterceptor(logging)                      // 日志拦截
                .authenticator(tokenAuthenticator)            // 401 自动刷新 Token
                .certificatePinner(certificatePinnerOrDefault())  // P2-07：证书锁定（HTTPS 上线后启用）
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS) // P2-07 已预备：避免上传大附件时被默认 10s 切断
                .build();
    }

    /** P2-07：CertificatePinner 不可为 null（OkHttp 接受 DEFAULT 表示禁用）. */
    private static okhttp3.CertificatePinner certificatePinnerOrDefault() {
        okhttp3.CertificatePinner pinner = CertificatePinnerProvider.provide();
        return pinner != null ? pinner : okhttp3.CertificatePinner.DEFAULT;
    }

    @Provides
    @Singleton
    static Retrofit provideRetrofit(OkHttpClient client) {
        // 通过 TypeAdapterFactory 修复 UTF-8 双重编码乱码（不干扰 Gson 类型系统）
        com.google.gson.Gson gson = new GsonBuilder()
                .registerTypeAdapterFactory(new Utf8FixTypeAdapterFactory())
                .create();

        // ─────────────────────────────────────────────────────────────────────
        // P0-02: 启动期协议校验
        //        ▸ Release 构建 + 非 https 前缀 → 立即抛错（防绕过 verifyReleaseHttps）
        //        ▸ Debug 构建仅 warn，不阻断（保留本地调试便利）
        // ─────────────────────────────────────────────────────────────────────
        String baseUrl = BuildConfig.BASE_URL;
        boolean isHttps = baseUrl != null && baseUrl.startsWith("https://");
        if (!BuildConfig.DEBUG && !isHttps) {
            throw new IllegalStateException(
                    "Release 构建检测到非 HTTPS BASE_URL：" + baseUrl
                            + "，请通过 local.properties 设置 api.use.https=true 后重新构建。");
        }
        if (!isHttps) {
            Log.w(TAG, "当前使用 HTTP BASE_URL=" + baseUrl
                    + "（仅 Debug 构建允许；HTTPS 部署后须切换为 https://）");
        } else {
            Log.i(TAG, "BASE_URL=" + baseUrl + " (HTTPS)");
        }

        return new Retrofit.Builder()
                .baseUrl(baseUrl)
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
