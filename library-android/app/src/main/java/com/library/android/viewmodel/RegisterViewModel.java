package com.library.android.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.library.android.model.RegisterRequest;
import com.library.android.model.Result;
import com.library.android.network.AuthApiService;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 注册页 ViewModel — 通过 Hilt 注入 AuthApiService.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@HiltViewModel
public class RegisterViewModel extends ViewModel {

    private static final String TAG = "RegisterViewModel";

    private final AuthApiService authApi;
    private final MutableLiveData<Boolean> registerSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final CompositeDisposable disposables = new CompositeDisposable();

    @Inject
    public RegisterViewModel(AuthApiService authApi) {
        this.authApi = authApi;
    }

    public LiveData<Boolean> isRegisterSuccess() { return registerSuccess; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> isLoading() { return loading; }

    public void register(String username, String password, String realName, String email, String phone) {
        if (username.isEmpty() || password.isEmpty() || realName.isEmpty() || email.isEmpty()) {
            errorMessage.setValue("请填写所有必填项");
            return;
        }

        loading.setValue(true);
        Log.d(TAG, "开始注册请求: username=" + username);

        disposables.add(
            authApi.register(new RegisterRequest(username, password, realName, email, phone))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    result -> {
                        loading.setValue(false);
                        if (result.isSuccess()) {
                            Log.d(TAG, "注册成功");
                            registerSuccess.setValue(true);
                        } else {
                            errorMessage.setValue(result.getMessage());
                        }
                    },
                    throwable -> {
                        loading.setValue(false);
                        Log.e(TAG, "注册网络请求失败", throwable);
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