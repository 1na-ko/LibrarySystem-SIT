package com.library.android.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.library.android.model.LoginResponse;
import com.library.android.model.Result;
import com.library.android.network.TokenManager;
import com.library.android.repository.AuthRepository;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 登录页 ViewModel — 通过 Hilt 注入 AuthRepository 与 TokenManager.
 *
 * <p>B.2 重构：原直接注入 AuthApiService 已被移除，统一通过 AuthRepository 调用，
 * 错误处理走 ApiCallExecutor 统一异常体系.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@HiltViewModel
public class LoginViewModel extends BaseViewModel {

    private static final String TAG = "LoginViewModel";

    private final AuthRepository authRepository;
    private final TokenManager tokenManager;

    private final MutableLiveData<Boolean> loginSuccess = new MutableLiveData<>();
    @Inject
    public LoginViewModel(AuthRepository authRepository, TokenManager tokenManager) {
        this.authRepository = authRepository;
        this.tokenManager = tokenManager;
    }

    public LiveData<Boolean> isLoginSuccess() { return loginSuccess; }
    public void login(String username, String password) {
        if (username.isEmpty() || password.isEmpty()) {
            Log.w(TAG, "用户名或密码为空");
            postError(new RuntimeException("请输入用户名和密码"));
            return;
        }

        setLoading(com.library.android.ui.common.LoadingState.LOADING);
        // 移除 PII 日志：原代码会打印 username 至 logcat（OWASP 违规）
        Log.d(TAG, "开始登录请求");

        Single<Result<LoginResponse>> single = authRepository.login(username, password);

        disposables.add(
            single.subscribeOn(Schedulers.io())
                  .observeOn(AndroidSchedulers.mainThread())
                  .subscribe(
                      result -> {
                          setLoading(com.library.android.ui.common.LoadingState.CONTENT);
                          Log.d(TAG, "登录响应: code=" + result.getCode() + ", success=" + result.isSuccess());
                          if (result.isSuccess() && result.getData() != null) {
                              LoginResponse resp = result.getData();
                              tokenManager.saveTokens(resp.getAccessToken(), resp.getRefreshToken());
                              // A.1 关键修复：补全角色 + 用户 ID 持久化
                              if (resp.getUser() != null) {
                                  tokenManager.saveUserInfo(username, resp.getUser().getRealName());
                                  tokenManager.saveUserRole(resp.getUser().getRole());
                                  tokenManager.saveUserId(resp.getUser().getId());
                              } else {
                                  tokenManager.saveUserInfo(username, null);
                              }
                              loginSuccess.setValue(true);
                          } else {
                              postError(new RuntimeException(result.getMessage()));
                          }
                      },
                      throwable -> {
                          setLoading(com.library.android.ui.common.LoadingState.CONTENT);
                          Log.e(TAG, "网络请求失败", throwable);
                          postError(new RuntimeException("网络错误：" + throwable.getMessage()));
                      }
                  )
        );
    }


}
