package com.library.android.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.library.android.model.BorrowRecordVO;
import com.library.android.model.BorrowStatsVO;
import com.library.android.model.PageResult;
import com.library.android.model.UserProfile;
import com.library.android.repository.AuthRepository;
import com.library.android.repository.UserRepository;
import com.library.android.ui.common.SingleLiveEvent;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 个人中心 ViewModel（WP4.1 精简：推荐相关逻辑已拆分至 HomeViewModel）.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@HiltViewModel
public class ProfileViewModel extends BaseViewModel {

    private static final String TAG = "ProfileViewModel";
    private static final int PAGE_SIZE = 20;

    private final UserRepository userRepository;
    private final AuthRepository authRepository;

    private final MutableLiveData<UserProfile> userProfile = new MutableLiveData<>();
    private final MutableLiveData<PageResult<BorrowRecordVO>> borrowHistory = new MutableLiveData<>();
    private final MutableLiveData<BorrowStatsVO> borrowStats = new MutableLiveData<>();
    private final MutableLiveData<Boolean> saveSuccess = new MutableLiveData<>();
    /** P1-01：登出完成事件（成功 / 后端不可达均触发，UI 收到后执行本地清退 + 跳登录）. */
    private final SingleLiveEvent<Object> logoutCompleted = new SingleLiveEvent<>();
    /** 登出请求是否进行中（UI 用于禁用按钮）. */
    private final MutableLiveData<Boolean> loggingOut = new MutableLiveData<>(false);


    @Inject
    public ProfileViewModel(UserRepository userRepository, AuthRepository authRepository) {
        this.userRepository = userRepository;
        this.authRepository = authRepository;
    }

    public LiveData<UserProfile> getUserProfile() { return userProfile; }
    public LiveData<PageResult<BorrowRecordVO>> getBorrowHistory() { return borrowHistory; }
    public LiveData<BorrowStatsVO> getBorrowStats() { return borrowStats; }
    public LiveData<Boolean> isSaveSuccess() { return saveSuccess; }
    public LiveData<Object> getLogoutCompleted() { return logoutCompleted; }
    public LiveData<Boolean> isLoggingOut() { return loggingOut; }

    /** 加载个人信息. */
    public void loadProfile() {
        setLoading(com.library.android.ui.common.LoadingState.LOADING);
        disposables.add(
            userRepository.getMyProfile()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    result -> {
                        setLoading(com.library.android.ui.common.LoadingState.CONTENT);
                        if (result.isSuccess() && result.getData() != null) {
                            userProfile.setValue(result.getData());
                        } else {
                            postError(new RuntimeException(result.getMessage()));
                        }
                    },
                    throwable -> {
                        setLoading(com.library.android.ui.common.LoadingState.CONTENT);
                        Log.e(TAG, "加载个人信息失败", throwable);
                    }
                )
        );
    }

    /** 更新个人信息. */
    public void updateProfile(String email, String phone) {
        setLoading(com.library.android.ui.common.LoadingState.LOADING);
        Map<String, String> body = new HashMap<>();
        body.put("email", email);
        body.put("phone", phone);

        disposables.add(
            userRepository.updateMyProfile(body)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    result -> {
                        if (result.isSuccess()) {
                            saveSuccess.setValue(true);
                            loadProfile();
                        } else {
                            setLoading(com.library.android.ui.common.LoadingState.CONTENT);
                            postError(new RuntimeException(result.getMessage()));
                        }
                    },
                    throwable -> {
                        setLoading(com.library.android.ui.common.LoadingState.CONTENT);
                        Log.e(TAG, "更新个人信息失败", throwable);
                        postError(new RuntimeException("更新失败：" + throwable.getMessage()));
                    }
                )
        );
    }

    /** 加载借阅历史. */
    public void loadBorrowHistory(Integer year, int pageNum) {
        setLoading(com.library.android.ui.common.LoadingState.LOADING);
        disposables.add(
            userRepository.getMyHistory(year, pageNum, PAGE_SIZE)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    result -> {
                        setLoading(com.library.android.ui.common.LoadingState.CONTENT);
                        if (result.isSuccess() && result.getData() != null) {
                            borrowHistory.setValue(result.getData());
                        } else {
                            postError(new RuntimeException(result.getMessage()));
                        }
                    },
                    throwable -> {
                        setLoading(com.library.android.ui.common.LoadingState.CONTENT);
                        Log.e(TAG, "加载借阅历史失败", throwable);
                    }
                )
        );
    }

    /**
     * WP-8：借阅历史上拉加载更多（追加而非替换）.
     *
     * @param year    年份筛选（null=全部）
     * @param pageNum 下一页页码
     */
    public void loadBorrowHistoryMore(Integer year, int pageNum) {
        disposables.add(
            userRepository.getMyHistory(year, pageNum, PAGE_SIZE)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    result -> {
                        if (result.isSuccess() && result.getData() != null
                                && result.getData().getRecords() != null) {
                            com.library.android.model.PageResult<BorrowRecordVO> current =
                                    borrowHistory.getValue();
                            if (current != null) {
                                java.util.List<BorrowRecordVO> merged = new java.util.ArrayList<>(
                                        current.getRecords() != null ? current.getRecords() : java.util.Collections.emptyList());
                                merged.addAll(result.getData().getRecords());
                                borrowHistory.setValue(new com.library.android.model.PageResult<>(
                                        merged, result.getData().getTotal(), result.getData().getPageNum(),
                                        result.getData().getPageSize(), result.getData().getTotalPages()));
                            } else {
                                borrowHistory.setValue(result.getData());
                            }
                        }
                    },
                    throwable -> Log.e(TAG, "加载借阅历史更多失败", throwable)
                )
        );
    }

    /** WP-8：当前借阅历史分页信息. */
    public com.library.android.model.PageResult<BorrowRecordVO> getCurrentHistoryPage() {
        return borrowHistory.getValue();
    }

    /**
     * P1-01：登出 — 取代 ProfileFragment 内 authRepository.logout() 直接订阅.
     *
     * <p>无论后端 API 成功还是失败，都触发 logoutCompleted（与原 Fragment 内
     * "后端不可达时仍要保证本地清退"语义一致），UI 收到后清 token + 跳登录.
     */
    public void logout() {
        if (Boolean.TRUE.equals(loggingOut.getValue())) return;  // 防抖
        loggingOut.setValue(true);
        disposables.add(authRepository.logout()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            loggingOut.setValue(false);
                            logoutCompleted.setValue(new Object());
                        },
                        throwable -> {
                            // 后端不可达：仍发完成事件让 UI 执行本地清退
                            loggingOut.setValue(false);
                            Log.w(TAG, "后端 logout 失败，仍执行本地登出", throwable);
                            logoutCompleted.setValue(new Object());
                        }));
    }

    /** 加载借阅统计. */
    public void loadBorrowStats() {
        setLoading(com.library.android.ui.common.LoadingState.LOADING);
        disposables.add(
            userRepository.getMyStats()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    result -> {
                        setLoading(com.library.android.ui.common.LoadingState.CONTENT);
                        if (result.isSuccess() && result.getData() != null) {
                            borrowStats.setValue(result.getData());
                        } else {
                            postError(new RuntimeException(result.getMessage()));
                        }
                    },
                    throwable -> {
                        setLoading(com.library.android.ui.common.LoadingState.CONTENT);
                        Log.e(TAG, "加载借阅统计失败", throwable);
                    }
                )
        );
    }

}
