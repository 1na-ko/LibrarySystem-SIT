package com.library.android.repository;

import com.library.android.model.LoginRequest;
import com.library.android.model.LoginResponse;
import com.library.android.model.RegisterRequest;
import com.library.android.model.Result;
import com.library.android.network.ApiCallExecutor;
import com.library.android.network.LibraryApi;

import io.reactivex.rxjava3.core.Single;

/**
 * 认证 Repository — 登录/注册/登出.
 *
 * <p>Token 刷新由 {@link com.library.android.network.TokenAuthenticator} 独立负责，
 * 此处不再暴露 refreshToken 入口（避免双路径与并发竞态）.
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
        return Single.fromCallable(() -> ApiCallExecutor.execute(
                api.login(new LoginRequest(username, password))));
    }

    /** 后端注册成功返回 LoginResponse（含 token 对），可直接登录. */
    public Single<Result<LoginResponse>> register(String username, String password,
                                                  String realName, String email, String phone) {
        return Single.fromCallable(() -> ApiCallExecutor.execute(
                api.register(new RegisterRequest(username, password, realName, email, phone))));
    }

    /** 登出 — 命中后端 AT 黑名单（OWASP）. 网络失败时调用方仍应执行本地清退. */
    public Single<Result<Void>> logout() {
        return Single.fromCallable(() -> ApiCallExecutor.execute(api.logout()));
    }
}
