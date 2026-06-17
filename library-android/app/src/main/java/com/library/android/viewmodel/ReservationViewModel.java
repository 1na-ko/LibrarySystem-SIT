package com.library.android.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.library.android.model.PageResult;
import com.library.android.model.QueuePositionVO;
import com.library.android.model.ReservationVO;
import com.library.android.repository.ReservationRepository;
import com.library.android.ui.common.LoadingState;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 预约管理 ViewModel（人员 B 主导）.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@HiltViewModel
public class ReservationViewModel extends ViewModel {

    private final ReservationRepository repository;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private final MutableLiveData<LoadingState> loadingState = new MutableLiveData<>(LoadingState.LOADING);
    private final MutableLiveData<List<ReservationVO>> reservationList = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> cancelResult = new MutableLiveData<>();
    private final MutableLiveData<QueuePositionVO> queuePosition = new MutableLiveData<>();

    private int currentPage = 1;
    private int totalPages = 0;
    private String currentStatusFilter = null;
    private boolean isLoading = false;

    @Inject
    public ReservationViewModel(ReservationRepository repository) {
        this.repository = repository;
    }

    public LiveData<LoadingState> getLoadingState() { return loadingState; }
    public LiveData<List<ReservationVO>> getReservationList() { return reservationList; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getCancelResult() { return cancelResult; }
    public LiveData<QueuePositionVO> getQueuePosition() { return queuePosition; }

    /** 加载预约列表. */
    public void loadReservations(String status) {
        currentStatusFilter = status;
        currentPage = 1;
        loadingState.setValue(LoadingState.LOADING);

        disposables.add(repository.getMyReservations(status, currentPage, 20)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    if (result != null && result.isSuccess() && result.getData() != null) {
                        PageResult<ReservationVO> page = result.getData();
                        List<ReservationVO> records = page.getRecords();
                        reservationList.setValue(records != null ? records : new ArrayList<>());
                        totalPages = page.getTotalPages();
                        loadingState.setValue(records == null || records.isEmpty()
                                ? LoadingState.EMPTY : LoadingState.CONTENT);
                    } else {
                        errorMessage.setValue(result != null ? result.getMessage() : "加载失败");
                        loadingState.setValue(LoadingState.ERROR);
                    }
                }, throwable -> {
                    errorMessage.setValue(throwable.getMessage());
                    loadingState.setValue(LoadingState.ERROR);
                }));
    }

    /** 加载更多. */
    public void loadMore() {
        if (isLoading || currentPage >= totalPages) return;
        isLoading = true;
        currentPage++;
        disposables.add(repository.getMyReservations(currentStatusFilter, currentPage, 20)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    isLoading = false;
                    if (result != null && result.isSuccess() && result.getData() != null) {
                        List<ReservationVO> records = result.getData().getRecords();
                        List<ReservationVO> current = new ArrayList<>(reservationList.getValue() != null
                                ? reservationList.getValue() : new ArrayList<>());
                        if (records != null) {
                            current.addAll(records);
                        }
                        reservationList.setValue(current);
                    }
                }, throwable -> isLoading = false));
    }

    /** 取消预约. */
    public void cancelReservation(long reservationId) {
        disposables.add(repository.cancelReservation(reservationId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> cancelResult.setValue(result != null && result.isSuccess()),
                        throwable -> cancelResult.setValue(false)));
    }

    /** 查询排队位置. */
    public void queryQueuePosition(long reservationId) {
        disposables.add(repository.getQueuePosition(reservationId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    if (result != null && result.isSuccess() && result.getData() != null) {
                        queuePosition.setValue(result.getData());
                    }
                }, Throwable::printStackTrace));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
    }
}
