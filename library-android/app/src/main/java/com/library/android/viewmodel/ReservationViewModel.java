package com.library.android.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.library.android.model.PageResult;
import com.library.android.model.ReservationVO;
import com.library.android.repository.ReservationRepository;
import com.library.android.ui.common.LoadingState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 预约管理 ViewModel（人员 B 主导）.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@HiltViewModel
public class ReservationViewModel extends BaseViewModel {

    private final ReservationRepository repository;

    /** WP-5：删除遮蔽的 loadingState（继承自 BaseViewModel）. */
    private final MutableLiveData<List<ReservationVO>> reservationList = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> cancelResult = new MutableLiveData<>();
    private final MutableLiveData<Integer> queuePosition = new MutableLiveData<>();

    private int currentPage = 1;
    private int totalPages = 0;
    private String currentStatusFilter = null;
    private boolean isLoading = false;

    /**
     * 正在取消的预约 ID 集合（防抖）.
     *
     * <p>左滑删除场景下，快速重复滑动可能在 Snackbar 显示前再次触发取消，
     * 利用此集合在 ViewModel 层面拦截重复请求.
     */
    private final Set<Long> cancellingIds = new HashSet<>();

    @Inject
    public ReservationViewModel(ReservationRepository repository) {
        this.repository = repository;
    }

    public LiveData<List<ReservationVO>> getReservationList() { return reservationList; }
    public LiveData<Boolean> getCancelResult() { return cancelResult; }
    /** 排队序号（后端当前仅返回 Integer 序号，不含 totalWaiting）. */
    public LiveData<Integer> getQueuePosition() { return queuePosition; }

    /** 加载预约列表. */
    public void loadReservations(String status) {
        currentStatusFilter = status;
        currentPage = 1;
        setLoading(LoadingState.LOADING);

        disposables.add(repository.getMyReservations(status, currentPage, 20)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    if (result != null && result.isSuccess() && result.getData() != null) {
                        PageResult<ReservationVO> page = result.getData();
                        List<ReservationVO> records = page.getRecords();
                        reservationList.setValue(records != null ? records : new ArrayList<>());
                        totalPages = page.getTotalPages();
                        setLoading(records == null || records.isEmpty()
                                ? LoadingState.EMPTY : LoadingState.CONTENT);
                    } else {
                        postError(new RuntimeException(result != null ? result.getMessage() : "加载失败"));
                        setLoading(LoadingState.ERROR);
                    }
                }, throwable -> {
                    postError(new RuntimeException(throwable.getMessage()));
                    setLoading(LoadingState.ERROR);
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

    /** 取消预约（防抖：同一 ID 在请求未完成前再次调用会被忽略）. */
    public void cancelReservation(long reservationId) {
        if (!cancellingIds.add(reservationId)) {
            return;  // 该 ID 正在取消中
        }
        disposables.add(repository.cancelReservation(reservationId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                            cancellingIds.remove(reservationId);
                            boolean ok = result != null && result.isSuccess();
                            cancelResult.setValue(ok);
                            if (!ok) {
                                postError(new RuntimeException("取消预约失败：" +
                                        (result != null && result.getMessage() != null ? result.getMessage() : "未知错误")));
                            }
                        },
                        throwable -> {
                            cancellingIds.remove(reservationId);
                            cancelResult.setValue(false);
                            postError(new RuntimeException("取消预约失败：" +
                                    (throwable.getMessage() != null ? throwable.getMessage() : "未知错误")));
                        }));
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


}
