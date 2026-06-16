package com.library.android.network;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * OkHttp 认证拦截器 — 自动为所有请求附加 Authorization Bearer Token.
 *
 * <p>如果 Token 不可用（未登录），则放行原始请求（由后端返回 401 触发登录流程）。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public class AuthInterceptor implements Interceptor {

    private final TokenManager tokenManager;

    public AuthInterceptor(Context context) {
        this.tokenManager = TokenManager.getInstance(context);
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request original = chain.request();
        String token = tokenManager.getAccessToken();

        if (token != null && !token.isEmpty()) {
            Request authenticated = original.newBuilder()
                    .header("Authorization", "Bearer " + token)
                    .build();
            return chain.proceed(authenticated);
        }

        return chain.proceed(original);
    }
}
