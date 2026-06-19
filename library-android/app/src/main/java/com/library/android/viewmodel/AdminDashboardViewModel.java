package com.library.android.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.library.android.model.DashboardVO;
import com.library.android.repository.AdminRepository;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 管理端 Dashboard ViewModel — 加载流通统计数据.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@HiltViewModel
public class AdminDashboardViewModel extends BaseViewModel {

    private static final String TAG = "AdminDashboardVM";

    private final AdminRepository repository;

    private final MutableLiveData<DashboardVO> dashboard = new MutableLiveData<>();

    @Inject
    public AdminDashboardViewModel(AdminRepository repository) {
        this.repository = repository;
    }

    public LiveData<DashboardVO> getDashboard() {
        return dashboard;
    }

    public void load() {
        setLoading(com.library.android.ui.common.LoadingState.LOADING);
        disposables.add(repository.getDashboard()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            if (result != null && result.isSuccess() && result.getData() != null) {
                                dashboard.setValue(result.getData());
                                setLoading(com.library.android.ui.common.LoadingState.CONTENT);
                            } else {
                                setLoading(com.library.android.ui.common.LoadingState.ERROR);
                            }
                        },
                        throwable -> {
                            Log.e(TAG, "Dashboard 加载失败", throwable);
                            setLoading(com.library.android.ui.common.LoadingState.ERROR);
                            postError(throwable);
                        }));
    }
}
