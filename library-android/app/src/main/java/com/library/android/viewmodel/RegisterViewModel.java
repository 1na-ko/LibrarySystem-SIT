package com.library.android.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.library.android.repository.AuthRepository;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 注册页 ViewModel — 通过 Hilt 注入 AuthRepository.
 *
 * <p>B.2 重构：原直接注入 AuthApiService 已被移除，统一通过 AuthRepository 调用.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@HiltViewModel
public class RegisterViewModel extends BaseViewModel {

    private static final String TAG = "RegisterViewModel";

    private final AuthRepository authRepository;
    private final MutableLiveData<Boolean> registerSuccess = new MutableLiveData<>();
    @Inject
    public RegisterViewModel(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    public LiveData<Boolean> isRegisterSuccess() { return registerSuccess; }
    public void register(String username, String password, String realName, String email, String phone) {
        if (username.isEmpty() || password.isEmpty() || realName.isEmpty() || email.isEmpty()) {
            postError(new RuntimeException("请填写所有必填项"));
            return;
        }

        setLoading(com.library.android.ui.common.LoadingState.LOADING);
        // 移除 PII 日志：原代码会打印 username 至 logcat（OWASP 违规）
        Log.d(TAG, "开始注册请求");

        disposables.add(
            authRepository.register(username, password, realName, email, phone)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    result -> {
                        setLoading(com.library.android.ui.common.LoadingState.CONTENT);
                        if (result.isSuccess()) {
                            Log.d(TAG, "注册成功");
                            registerSuccess.setValue(true);
                        } else {
                            postError(new RuntimeException(result.getMessage()));
                        }
                    },
                    throwable -> {
                        setLoading(com.library.android.ui.common.LoadingState.CONTENT);
                        Log.e(TAG, "注册网络请求失败", throwable);
                        postError(new RuntimeException("网络错误：" + throwable.getMessage()));
                    }
                )
        );
    }


}
