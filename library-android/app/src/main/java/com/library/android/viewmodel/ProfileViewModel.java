package com.library.android.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.library.android.model.BorrowRecordVO;
import com.library.android.model.BorrowStatsVO;
import com.library.android.model.PageResult;
import com.library.android.model.UserProfile;
import com.library.android.repository.UserRepository;

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

    private final MutableLiveData<UserProfile> userProfile = new MutableLiveData<>();
    private final MutableLiveData<PageResult<BorrowRecordVO>> borrowHistory = new MutableLiveData<>();
    private final MutableLiveData<BorrowStatsVO> borrowStats = new MutableLiveData<>();
    private final MutableLiveData<Boolean> saveSuccess = new MutableLiveData<>();


    @Inject
    public ProfileViewModel(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public LiveData<UserProfile> getUserProfile() { return userProfile; }
    public LiveData<PageResult<BorrowRecordVO>> getBorrowHistory() { return borrowHistory; }
    public LiveData<BorrowStatsVO> getBorrowStats() { return borrowStats; }
    public LiveData<Boolean> isSaveSuccess() { return saveSuccess; }

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
