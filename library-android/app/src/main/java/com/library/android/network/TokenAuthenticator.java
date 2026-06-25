package com.library.android.network;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.library.android.model.RefreshRequest;
import com.library.android.model.RefreshResponse;
import com.library.android.model.Result;

import java.io.IOException;

import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import retrofit2.Call;

/**
 * OkHttp Authenticator — 当收到 401 响应时自动尝试刷新 Token.
 *
 * <p>使用同步请求刷新，成功后更新存储的 Token 并重试原请求。
 * 刷新失败则清除 Token（触发重新登录）。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public class TokenAuthenticator implements Authenticator {

    private final TokenManager tokenManager;
    private final LibraryApiProvider apiProvider;
    private final SessionManager sessionManager;

    public TokenAuthenticator(Context context, LibraryApiProvider apiProvider, SessionManager sessionManager) {
        this.tokenManager = TokenManager.getInstance(context);
        this.apiProvider = apiProvider;
        this.sessionManager = sessionManager;
    }

    @Nullable
    @Override
    public Request authenticate(@Nullable Route route, @NonNull Response response) {
        // WP 修复：refreshToken 请求本身返回 401 时直接放弃（避免无限递归 → 栈溢出 SIGSEGV）
        // 之前 refreshToken 过期 → 401 → Authenticator 再次 refreshToken → 401 → ... 无限递归
        String path = response.request().url().encodedPath();
        if (path != null && path.contains("/auth/refresh")) {
            tokenManager.clear();
            sessionManager.notifyExpired();
            return null;
        }

        String refreshToken = tokenManager.getRefreshToken();
        if (refreshToken == null) {
            return null;
        }

        // 防止并发刷新——已带新 Token 的请求不再刷新
        if (response.request().header("Authorization") != null) {
            String currentToken = tokenManager.getAccessToken();
            String requestToken = response.request().header("Authorization");
            if (requestToken != null && !requestToken.equals("Bearer " + currentToken)) {
                return null;
            }
        }

        synchronized (this) {
            // 二次检查：可能已被其他线程刷新
            String latestToken = tokenManager.getAccessToken();
            String sentToken = response.request().header("Authorization");
            if (sentToken != null && latestToken != null && !sentToken.equals("Bearer " + latestToken)) {
                return response.request().newBuilder()
                        .header("Authorization", "Bearer " + latestToken)
                        .build();
            }

            try {
                LibraryApi api = apiProvider.getApi();
                if (api == null) {
                    return null;  // DI 尚未完成初始化，放弃本次刷新
                }
                Call<Result<RefreshResponse>> call = api.refreshToken(new RefreshRequest(refreshToken));
                retrofit2.Response<Result<RefreshResponse>> refreshResponse = call.execute();

                if (refreshResponse.isSuccessful() && refreshResponse.body() != null
                        && refreshResponse.body().isSuccess()) {
                    RefreshResponse data = refreshResponse.body().getData();
                    if (data != null) {
                        tokenManager.saveTokens(data.getAccessToken(), data.getRefreshToken());
                        return response.request().newBuilder()
                                .header("Authorization", "Bearer " + data.getAccessToken())
                                .build();
                    }
                }
            } catch (IOException e) {
                // 网络异常，放弃刷新（refresh token 仍可能可用，不立即标记会话失效）
                return null;
            }

            // 刷新失败（业务级失败：refresh token 过期/吊销/被替换）→ 清除本地凭据并广播会话失效
            tokenManager.clear();
            sessionManager.notifyExpired();
            return null;
        }
    }

    /** 提供 LibraryApi 实例，避免循环依赖. */
    public interface LibraryApiProvider {
        LibraryApi getApi();
    }
}
