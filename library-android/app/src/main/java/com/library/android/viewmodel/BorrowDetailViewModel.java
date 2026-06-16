package com.library.android.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.library.android.model.BorrowRecordVO;
import com.library.android.repository.BorrowRepository;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 借阅详情 ViewModel（人员 B 主导）.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@HiltViewModel
public class BorrowDetailViewModel extends ViewModel {

    private final BorrowRepository repository;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private final MutableLiveData<BorrowRecordVO> borrowDetail = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(true);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> returnSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> renewResult = new MutableLiveData<>();

    @Inject
    public BorrowDetailViewModel(BorrowRepository repository) {
        this.repository = repository;
    }

    public LiveData<BorrowRecordVO> getBorrowDetail() { return borrowDetail; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getReturnSuccess() { return returnSuccess; }
    public LiveData<String> getRenewResult() { return renewResult; }

    /** 加载借阅详情. */
    public void loadDetail(long borrowId) {
        loading.setValue(true);
        disposables.add(repository.getBorrowDetail(borrowId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    loading.setValue(false);
                    if (result != null && result.isSuccess() && result.getData() != null) {
                        borrowDetail.setValue(result.getData());
                    } else {
                        errorMessage.setValue(result != null ? result.getMessage() : "加载失败");
                    }
                }, throwable -> {
                    loading.setValue(false);
                    errorMessage.setValue(throwable.getMessage());
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

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
    }
}
