package com.library.android.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.library.android.ui.common.LoadingState;
import com.library.android.ui.common.SingleLiveEvent;

import io.reactivex.rxjava3.disposables.CompositeDisposable;

/**
 * ViewModel 基类 — 集中管理 RxJava 订阅、加载状态、错误事件.
 *
 * <p>子类应:
 * <ol>
 *   <li>使用 {@link #disposables} 持有所有订阅，无需手写 onCleared.</li>
 *   <li>通过 {@link #errorEvent} 投递异常给 UI 层做统一展示（避免每个 Fragment 自己写 Snackbar）.</li>
 *   <li>通过 {@link #loadingState} 暴露加载状态，UI 层观察显示 progress bar.</li>
 * </ol>
 *
 * <p>{@link SingleLiveEvent} 保证错误只触发一次（避免 Fragment 重建时弹两次 Snackbar）.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public abstract class BaseViewModel extends ViewModel {

    protected final CompositeDisposable disposables = new CompositeDisposable();

    private final MutableLiveData<LoadingState> loadingState = new MutableLiveData<>(LoadingState.IDLE);
    private final SingleLiveEvent<Throwable> errorEvent = new SingleLiveEvent<>();

    public LiveData<LoadingState> getLoadingState() {
        return loadingState;
    }

    public LiveData<Throwable> getErrorEvent() {
        return errorEvent;
    }

    protected void setLoading(LoadingState state) {
        loadingState.postValue(state);
    }

    protected void postError(Throwable throwable) {
        errorEvent.postValue(throwable);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
    }
}
