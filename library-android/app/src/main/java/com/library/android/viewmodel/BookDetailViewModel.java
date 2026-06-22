package com.library.android.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.library.android.model.BookDetailVO;
import com.library.android.model.BookRecommendVO;
import com.library.android.model.ReservationVO;
import com.library.android.repository.BookRepository;
import com.library.android.repository.ReservationRepository;
import com.library.android.ui.common.LoadingState;
import com.library.android.ui.common.SingleLiveEvent;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 图书详情页 ViewModel.
 *
 * <p>P1-01：将原 BookDetailFragment 直接注入的 ReservationRepository 调用下沉至此，
 * UI 层只观察 reserveResult LiveData 并做相应反馈.
 */
@HiltViewModel
public class BookDetailViewModel extends BaseViewModel {

    private static final String TAG = "BookDetailViewModel";

    private final BookRepository bookRepository;
    private final ReservationRepository reservationRepository;

    private final MutableLiveData<BookDetailVO> bookDetail = new MutableLiveData<>();
    private final MutableLiveData<List<BookRecommendVO>> relatedBooks = new MutableLiveData<>();
    /** 预约成功事件（含排队位置）— SingleLiveEvent 防 Fragment 重建重复弹 Snackbar. */
    private final SingleLiveEvent<ReservationVO> reserveSuccess = new SingleLiveEvent<>();
    /** 预约请求是否进行中（UI 用于禁用按钮防抖）. */
    private final MutableLiveData<Boolean> reserving = new MutableLiveData<>(false);

    @Inject
    public BookDetailViewModel(BookRepository bookRepository,
                               ReservationRepository reservationRepository) {
        this.bookRepository = bookRepository;
        this.reservationRepository = reservationRepository;
    }

    public LiveData<BookDetailVO> getBookDetail() { return bookDetail; }
    public LiveData<List<BookRecommendVO>> getRelatedBooks() { return relatedBooks; }
    public LiveData<ReservationVO> getReserveSuccess() { return reserveSuccess; }
    public LiveData<Boolean> isReserving() { return reserving; }

    public void loadBookDetail(long bookId) {
        setLoading(LoadingState.LOADING);
        disposables.add(
            bookRepository.getBookDetail(bookId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    result -> {
                        if (result.isSuccess() && result.getData() != null) {
                            bookDetail.setValue(result.getData());
                            setLoading(LoadingState.CONTENT);
                        } else {
                            setLoading(LoadingState.ERROR);
                            postError(new RuntimeException(result.getMessage()));
                        }
                    },
                    throwable -> {
                        setLoading(LoadingState.ERROR);
                        Log.e(TAG, "加载图书详情失败", throwable);
                        postError(throwable);
                    }
                )
        );

        disposables.add(
            bookRepository.getRelatedBooks(bookId, 5)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    result -> {
                        if (result.isSuccess() && result.getData() != null) {
                            relatedBooks.setValue(result.getData());
                        }
                    },
                    throwable -> Log.e(TAG, "加载相关推荐失败", throwable)
                )
        );
    }

    /**
     * P1-01：预约图书 — 取代原 BookDetailFragment 内 reservationRepository.reserveBook 直接订阅.
     *
     * <p>UI 通过 {@link #isReserving()} 控制按钮禁用，通过 {@link #getReserveSuccess()}
     * 接收成功事件，通过 {@link #getErrorEvent()} 接收失败/业务异常.
     */
    public void reserveBook(long bookId) {
        if (Boolean.TRUE.equals(reserving.getValue())) return;  // 防抖
        reserving.setValue(true);
        disposables.add(
            reservationRepository.reserveBook(bookId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    result -> {
                        reserving.setValue(false);
                        if (result.isSuccess() && result.getData() != null) {
                            reserveSuccess.setValue(result.getData());
                            // 预约成功后刷新详情（更新预约人数）
                            loadBookDetail(bookId);
                        } else {
                            postError(new RuntimeException(
                                    result.getMessage() != null ? result.getMessage() : "预约失败"));
                        }
                    },
                    throwable -> {
                        reserving.setValue(false);
                        Log.e(TAG, "预约失败", throwable);
                        postError(throwable);
                    }
                )
        );
    }
}
