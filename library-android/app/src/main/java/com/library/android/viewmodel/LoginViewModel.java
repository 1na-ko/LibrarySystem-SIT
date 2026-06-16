package com.library.android.viewmodel;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.library.android.model.LoginRequest;
import com.library.android.model.LoginResponse;
import com.library.android.model.Result;
import com.library.android.network.RetrofitClient;
import com.library.android.util.TokenManager;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class LoginViewModel extends ViewModel {

    private static final String TAG = "LoginViewModel";
    private final MutableLiveData<Boolean> loginSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    private final CompositeDisposable disposables = new CompositeDisposable();

    public LiveData<Boolean> isLoginSuccess() { return loginSuccess; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> isLoading() { return loading; }

    public void login(Context context, String username, String password) {
        if (username.isEmpty() || password.isEmpty()) {
            Log.w(TAG, "用户名或密码为空");
            errorMessage.setValue("请输入用户名和密码");
            return;
        }

        loading.setValue(true);
        Log.d(TAG, "开始登录请求: username=" + username);

        Single<Result<LoginResponse>> single = RetrofitClient.getAuthApi()
                .login(new LoginRequest(username, password));

        disposables.add(
            single.subscribeOn(Schedulers.io())
                  .observeOn(AndroidSchedulers.mainThread())
                  .subscribe(
                      result -> {
                          loading.setValue(false);
                          Log.d(TAG, "登录响应: code=" + result.getCode() + ", success=" + result.isSuccess());
                          if (result.isSuccess() && result.getData() != null) {
                              LoginResponse resp = result.getData();
                              TokenManager tm = TokenManager.getInstance(context);
                              tm.saveTokens(resp.getAccessToken(), resp.getRefreshToken());
                              tm.saveUserInfo(username, resp.getUser().getRealName());
                              Toast.makeText(context, "登录成功", Toast.LENGTH_SHORT).show();
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