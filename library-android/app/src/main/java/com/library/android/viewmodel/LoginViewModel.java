package com.library.android.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.library.android.model.LoginRequest;
import com.library.android.model.LoginResponse;
import com.library.android.model.Result;
import com.library.android.network.AuthApiService;
import com.library.android.util.TokenManager;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 登录页 ViewModel — 通过 Hilt 注入 AuthApiService 和 TokenManager.
 *
 * <p>不再需要 Context 参数，完全由构造函数注入依赖.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@HiltViewModel
public class LoginViewModel extends ViewModel {

    private static final String TAG = "LoginViewModel";

    private final AuthApiService authApi;
    private final TokenManager tokenManager;

    private final MutableLiveData<Boolean> loginSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final CompositeDisposable disposables = new CompositeDisposable();

    @Inject
    public LoginViewModel(AuthApiService authApi, TokenManager tokenManager) {
        this.authApi = authApi;
        this.tokenManager = tokenManager;
    }

    public LiveData<Boolean> isLoginSuccess() { return loginSuccess; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> isLoading() { return loading; }

    public void login(String username, String password) {
        if (username.isEmpty() || password.isEmpty()) {
            Log.w(TAG, "用户名或密码为空");
            errorMessage.setValue("请输入用户名和密码");
            return;
        }

        loading.setValue(true);
        Log.d(TAG, "开始登录请求: username=" + username);

        Single<Result<LoginResponse>> single = authApi.login(new LoginRequest(username, password));

        disposables.add(
            single.subscribeOn(Schedulers.io())
                  .observeOn(AndroidSchedulers.mainThread())
                  .subscribe(
                      result -> {
                          loading.setValue(false);
                          Log.d(TAG, "登录响应: code=" + result.getCode() + ", success=" + result.isSuccess());
                          if (result.isSuccess() && result.getData() != null) {
                              LoginResponse resp = result.getData();
                              tokenManager.saveTokens(resp.getAccessToken(), resp.getRefreshToken());
                              tokenManager.saveUserInfo(username, resp.getUser().getRealName());
                              loginSuccess.setValue(true);
                          } else {
                              errorMessage.setValue(result.getMessage());
                          }
                      },
                      throwable -> {
                          loading.setValue(false);
                          Log.e(TAG, "网络请求失败", throwable);
                          errorMessage.setValue("网络错误：" + throwable.getMessage());
                      }
                  )
        );
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
    }
}