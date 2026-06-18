package com.library.android.repository;

import com.library.android.model.LoginRequest;
import com.library.android.model.LoginResponse;
import com.library.android.model.RefreshRequest;
import com.library.android.model.RefreshResponse;
import com.library.android.model.RegisterRequest;
import com.library.android.model.Result;
import com.library.android.network.LibraryApi;

import io.reactivex.rxjava3.core.Single;

/**
 * 认证 Repository — 登录/注册/Token 刷新/登出.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public class AuthRepository {

    private final LibraryApi api;

    public AuthRepository(LibraryApi api) {
        this.api = api;
    }

    public Single<Result<LoginResponse>> login(String username, String password) {
        return Single.fromCallable(() -> api.login(new LoginRequest(username, password)).execute().body());
    }

    /** 后端注册即登录，返回 LoginResponse（含 token 对）. */
    public Single<Result<LoginResponse>> register(String username, String password, String realName, String email, String phone) {
        return Single.fromCallable(() ->
                api.register(new RegisterRequest(username, password, realName, email, phone)).execute().body());
    }

    public Single<Result<Void>> logout() {
        return Single.fromCallable(() -> api.logout().execute().body());
    }

    public Single<Result<RefreshResponse>> refreshToken(String refreshToken) {
        return Single.fromCallable(() -> api.refreshToken(new RefreshRequest(refreshToken)).execute().body());
    }
}
