package com.library.android.network;

import android.content.Context;
import android.util.Log;

import com.library.android.util.TokenManager;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {

    private static final String TAG = "AuthInterceptor";
    private final TokenManager tokenManager;

    public AuthInterceptor(Context context) {
        this.tokenManager = TokenManager.getInstance(context);
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();
        String token = tokenManager.getAccessToken();

        if (token == null) {
            Log.d(TAG, "No token, proceeding without auth: " + original.url().encodedPath());
            return chain.proceed(original);
        }

        Request request = original.newBuilder()
                .header("Authorization", "Bearer " + token)
                .build();
        Log.d(TAG, "Added Bearer token to: " + original.url().encodedPath());

        Response response = chain.proceed(request);

        // 处理 401 — 需要补充
        if (response.code() == 401) {
            Log.d(TAG, "Received 401, token may be expired");
            // TODO: 实现 Token 自动刷新后重试
        }

        return response;
    }
}