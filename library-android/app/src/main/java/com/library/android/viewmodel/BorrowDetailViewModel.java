package com.library.android.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.library.android.model.BorrowRecordVO;
import com.library.android.repository.BorrowRepository;
import com.library.android.ui.common.LoadingState;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 借阅详情 ViewModel（WP2.5：迁移至 BaseViewModel，保留旧 getter 兼容 Fragment）.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@HiltViewModel
public class BorrowDetailViewModel extends BaseViewModel {

    private final BorrowRepository repository;

    private final MutableLiveData<BorrowRecordVO> borrowDetail = new MutableLiveData<>();
    private final MutableLiveData<Boolean> returnSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> renewResult = new MutableLiveData<>();

    @Inject
    public BorrowDetailViewModel(BorrowRepository repository) {
        this.repository = repository;
    }

    public LiveData<BorrowRecordVO> getBorrowDetail() { return borrowDetail; }
    public LiveData<Boolean> getReturnSuccess() { return returnSuccess; }
    public LiveData<String> getRenewResult() { return renewResult; }

    /** 加载借阅详情. */
    public void loadDetail(long borrowId) {
        setLoading(com.library.android.ui.common.LoadingState.LOADING);
        setLoading(LoadingState.LOADING);
        disposables.add(repository.getBorrowDetail(borrowId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    setLoading(com.library.android.ui.common.LoadingState.CONTENT);
                    setLoading(LoadingState.CONTENT);
                    if (result != null && result.isSuccess() && result.getData() != null) {
                        borrowDetail.setValue(result.getData());
                    } else {
                        String msg = result != null ? result.getMessage() : "加载失败";
                        postError(new RuntimeException(msg));
                    }
                }, throwable -> {
                    setLoading(com.library.android.ui.common.LoadingState.CONTENT);
                    setLoading(LoadingState.ERROR);
                    postError(throwable);
                }));
    }

    /** 归还. */
    public void returnBook(long borrowId) {
        disposables.add(repository.returnBook(borrowId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> returnSuccess.setValue(result != null && result.isSuccess()),
                        throwable -> returnSuccess.setValue(false)));
    }

    /** 续借. */
    public void renewBook(long borrowId) {
        disposables.add(repository.renewBook(borrowId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    if (result != null && result.isSuccess() && result.getData() != null) {
                        renewResult.setValue(result.getData().getNewDueDate());
                    } else {
                        renewResult.setValue(null);
                    }
                }, throwable -> renewResult.setValue(null)));
    }
}
