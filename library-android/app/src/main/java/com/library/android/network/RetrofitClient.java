package com.library.android.network;

import android.content.Context;

import com.library.android.BuildConfig;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static Retrofit retrofit;
    private static AuthApiService authApiService;

    private RetrofitClient() {}

    public static synchronized void init(Context context) {
        if (retrofit != null) return;

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(BuildConfig.DEBUG
                ? HttpLoggingInterceptor.Level.BODY
                : HttpLoggingInterceptor.Level.NONE);

        // 模拟拦截器（不依赖后端时可开启，MockInterceptor 需在 AuthInterceptor 之前注册）
        MockInterceptor mockInterceptor = new MockInterceptor();
        if (BuildConfig.DEBUG) {
            mockInterceptor.setEnabled(false); // 设为 true 启用模拟
        }

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(mockInterceptor)         // 模拟拦截（优先拦截）
                .addInterceptor(new AuthInterceptor(context)) // 认证拦截
                .addInterceptor(logging)                 // 日志拦截
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl(BuildConfig.BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .build();

        authApiService = retrofit.create(AuthApiService.class);
    }

    public static AuthApiService getAuthApi() {
        if (authApiService == null) {
            throw new IllegalStateException("RetrofitClient not initialized. Call init() first.");
        }
        return authApiService;
    }
}
