package com.library.android.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.library.android.model.BookRecommendVO;
import com.library.android.model.BorrowRecordVO;
import com.library.android.model.BorrowStatsVO;
import com.library.android.model.PageResult;
import com.library.android.model.Result;
import com.library.android.model.UserProfile;
import com.library.android.repository.UserRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 个人中心 ViewModel.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@HiltViewModel
public class ProfileViewModel extends ViewModel {

    private static final String TAG = "ProfileViewModel";
    private static final int PAGE_SIZE = 20;

    private final UserRepository userRepository;

    private final MutableLiveData<UserProfile> userProfile = new MutableLiveData<>();
    private final MutableLiveData<PageResult<BorrowRecordVO>> borrowHistory = new MutableLiveData<>();
    private final MutableLiveData<BorrowStatsVO> borrowStats = new MutableLiveData<>();
    private final MutableLiveData<List<BookRecommendVO>> recommendations = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> saveSuccess = new MutableLiveData<>();

    private final CompositeDisposable disposables = new CompositeDisposable();

    @Inject
    public ProfileViewModel(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public LiveData<UserProfile> getUserProfile() { return userProfile; }
    public LiveData<PageResult<BorrowRecordVO>> getBorrowHistory() { return borrowHistory; }
    public LiveData<BorrowStatsVO> getBorrowStats() { return borrowStats; }
    public LiveData<List<BookRecommendVO>> getRecommendations() { return recommendations; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> isLoading() { return loading; }
    public LiveData<Boolean> isSaveSuccess() { return saveSuccess; }

    /** 加载个人信息. */
    public void loadProfile() {
        loading.setValue(true);
        disposables.add(
            userRepository.getMyProfile()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    result -> {
                        loading.setValue(false);
                        if (result.isSuccess() && result.getData() != null) {
                            userProfile.setValue(result.getData());
                        } else {
                            errorMessage.setValue(result.getMessage());
                        }
                    },
                    throwable -> {
                        loading.setValue(false);
                        Log.e(TAG, "加载个人信息失败", throwable);
                    }
                )
        );
    }

    /** 更新个人信息（后端返回 Void，成功后重新加载 profile）. */
    public void updateProfile(String email, String phone) {
        loading.setValue(true);
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
                            // 后端返回 Void，需要重新拉取个人信息
                            loadProfile();
                        } else {
                            loading.setValue(false);
                            errorMessage.setValue(result.getMessage());
                        }
                    },
                    throwable -> {
                        loading.setValue(false);
                        Log.e(TAG, "更新个人信息失败", throwable);
                        errorMessage.setValue("更新失败：" + throwable.getMessage());
                    }
                )
        );
    }

    /** 加载借阅历史. */
    public void loadBorrowHistory(Integer year, int pageNum) {
        loading.setValue(true);
        disposables.add(
            userRepository.getMyHistory(year, pageNum, PAGE_SIZE)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    result -> {
                        loading.setValue(false);
                        if (result.isSuccess() && result.getData() != null) {
                            borrowHistory.setValue(result.getData());
                        } else {
                            errorMessage.setValue(result.getMessage());
                        }
                    },
                    throwable -> {
                        loading.setValue(false);
                        Log.e(TAG, "加载借阅历史失败", throwable);
                    }
                )
        );
    }

    /** 加载借阅统计. */
    public void loadBorrowStats() {
        loading.setValue(true);
        disposables.add(
            userRepository.getMyStats()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    result -> {
                        loading.setValue(false);
                        if (result.isSuccess() && result.getData() != null) {
                            borrowStats.setValue(result.getData());
                        } else {
                            errorMessage.setValue(result.getMessage());
                        }
                    },
                    throwable -> {
                        loading.setValue(false);
                        Log.e(TAG, "加载借阅统计失败", throwable);
                    }
                )
        );
    }

    /** 加载个性化推荐. */
    public void loadRecommendations() {
        loading.setValue(true);
        disposables.add(
            userRepository.getRecommendations(20)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    result -> {
                        loading.setValue(false);
                        if (result.isSuccess() && result.getData() != null) {
                            recommendations.setValue(result.getData());
                        } else {
                            errorMessage.setValue(result.getMessage());
                        }
                    },
                    throwable -> {
                        loading.setValue(false);
                        Log.e(TAG, "加载推荐失败", throwable);
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