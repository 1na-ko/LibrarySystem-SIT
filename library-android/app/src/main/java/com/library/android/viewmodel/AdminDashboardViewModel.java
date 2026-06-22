package com.library.android.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.library.android.model.DashboardVO;
import com.library.android.repository.AdminRepository;
import com.library.android.ui.common.SingleLiveEvent;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 管理端 Dashboard ViewModel — 加载流通统计数据 + 全图谱重建.
 *
 * <p>P1-01：将原 AdminDashboardFragment 直接 {@code @Inject AdminRepository}
 * 调用 rebuildKgAll 的逻辑下沉至此，UI 通过 rebuildResult / rebuildInProgress
 * 三路 LiveData 观察.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@HiltViewModel
public class AdminDashboardViewModel extends BaseViewModel {

    private static final String TAG = "AdminDashboardVM";

    private final AdminRepository repository;

    private final MutableLiveData<DashboardVO> dashboard = new MutableLiveData<>();
    /** 图谱重建结果（处理书目数量），SingleLiveEvent 防 Snackbar 重复. */
    private final SingleLiveEvent<Integer> rebuildResult = new SingleLiveEvent<>();
    /** 图谱重建是否进行中，UI 用于禁用按钮 + 显示进度条. */
    private final MutableLiveData<Boolean> rebuildInProgress = new MutableLiveData<>(false);

    @Inject
    public AdminDashboardViewModel(AdminRepository repository) {
        this.repository = repository;
    }

    public LiveData<DashboardVO> getDashboard() { return dashboard; }
    public LiveData<Integer> getRebuildResult() { return rebuildResult; }
    public LiveData<Boolean> isRebuildInProgress() { return rebuildInProgress; }

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

    /** P1-01：触发全图谱重建（取代 Fragment 内 adminRepository.rebuildKgAll 直接订阅）. */
    public void rebuildKnowledgeGraph() {
        if (Boolean.TRUE.equals(rebuildInProgress.getValue())) return;  // 防抖
        rebuildInProgress.setValue(true);
        disposables.add(repository.rebuildKgAll()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            rebuildInProgress.setValue(false);
                            if (result != null && result.isSuccess()) {
                                int processed = result.getData() != null ? result.getData() : 0;
                                rebuildResult.setValue(processed);
                            } else {
                                postError(new RuntimeException(
                                        result != null ? result.getMessage() : "重建失败"));
                            }
                        },
                        throwable -> {
                            rebuildInProgress.setValue(false);
                            Log.e(TAG, "图谱重建失败", throwable);
                            postError(throwable);
                        }));
    }
}
