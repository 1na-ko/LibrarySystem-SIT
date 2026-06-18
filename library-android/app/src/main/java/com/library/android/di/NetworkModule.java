package com.library.android.di;

import android.content.Context;

import com.google.gson.GsonBuilder;
import com.library.android.BuildConfig;
import com.library.android.network.AuthApiService;
import com.library.android.network.AuthInterceptor;
import com.library.android.network.LibraryApi;
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

    private static final int TIMEOUT_SECONDS = 15;
    /** 打破 TokenAuthenticator ↔ Retrofit 循环依赖的持有者. */
    private static volatile LibraryApi sLibraryApi;

    @Provides
    @Singleton
    static TokenAuthenticator provideTokenAuthenticator(@ApplicationContext Context context) {
        return new TokenAuthenticator(context, () -> sLibraryApi);
    }

    @Provides
    @Singleton
    static OkHttpClient provideOkHttpClient(@ApplicationContext Context context,
                                            TokenAuthenticator tokenAuthenticator) {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(BuildConfig.DEBUG
                ? HttpLoggingInterceptor.Level.BODY
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
    static AuthApiService provideAuthApiService(Retrofit retrofit) {
        return retrofit.create(AuthApiService.class);
    }

    @Provides
    @Singleton
    static LibraryApi provideLibraryApi(Retrofit retrofit) {
        LibraryApi api = retrofit.create(LibraryApi.class);
        sLibraryApi = api;  // 注入到 TokenAuthenticator 的 LibraryApiProvider
        return api;
    }
}