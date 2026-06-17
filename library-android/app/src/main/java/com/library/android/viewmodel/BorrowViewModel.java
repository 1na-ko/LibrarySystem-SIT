package com.library.android.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.library.android.model.BorrowRecordVO;
import com.library.android.model.PageResult;
import com.library.android.model.Result;
import com.library.android.repository.BorrowRepository;
import com.library.android.ui.common.LoadingState;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 借阅管理 ViewModel（人员 B 主导）.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@HiltViewModel
public class BorrowViewModel extends ViewModel {

    private final BorrowRepository repository;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private final MutableLiveData<LoadingState> loadingState = new MutableLiveData<>(LoadingState.LOADING);
    private final MutableLiveData<List<BorrowRecordVO>> borrowList = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private int currentPage = 1;
    private int totalPages = 0;
    private String currentStatusFilter = null;
    private boolean isLoading = false;

    @Inject
    public BorrowViewModel(BorrowRepository repository) {
        this.repository = repository;
    }

    public LiveData<LoadingState> getLoadingState() { return loadingState; }
    public LiveData<List<BorrowRecordVO>> getBorrowList() { return borrowList; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    /** 加载借阅列表（首次加载或状态切换）. */
    public void loadBorrows(String status) {
        currentStatusFilter = status;
        currentPage = 1;
        loadingState.setValue(LoadingState.LOADING);

        disposables.add(repository.getMyBorrows(status, currentPage, 20)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    if (result != null && result.isSuccess() && result.getData() != null) {
                        PageResult<BorrowRecordVO> page = result.getData();
                        List<BorrowRecordVO> records = page.getRecords();
                        borrowList.setValue(records != null ? records : new ArrayList<>());
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

    /** 加载更多（分页）. */
    public void loadMore() {
        if (isLoading || currentPage >= totalPages) return;
        isLoading = true;
        currentPage++;

        disposables.add(repository.getMyBorrows(currentStatusFilter, currentPage, 20)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    isLoading = false;
                    if (result != null && result.isSuccess() && result.getData() != null) {
                        List<BorrowRecordVO> records = result.getData().getRecords();
                        List<BorrowRecordVO> current = new ArrayList<>(borrowList.getValue() != null
                                ? borrowList.getValue() : new ArrayList<>());
                        if (records != null) {
                            current.addAll(records);
                        }
                        borrowList.setValue(current);
                    }
                }, throwable -> isLoading = false));
    }

    /** 归还图书. */
    public LiveData<Boolean> returnBook(long borrowId) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        disposables.add(repository.returnBook(borrowId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(r -> result.setValue(r != null && r.isSuccess()),
                        throwable -> result.setValue(false)));
        return result;
    }

    /** 借书申请. */
    public LiveData<Boolean> borrowBook(long bookId) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        disposables.add(repository.borrowBook(bookId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(r -> result.setValue(r != null && r.isSuccess()),
                        throwable -> result.setValue(false)));
        return result;
    }

    /** 续借图书. */
    public LiveData<String> renewBook(long borrowId) {
        MutableLiveData<String> result = new MutableLiveData<>();
        disposables.add(repository.renewBook(borrowId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(r -> {
                    if (r != null && r.isSuccess() && r.getData() != null) {
                        result.setValue(r.getData().getNewDueDate());
                    } else {
                        result.setValue(null);
                    }
                }, throwable -> result.setValue(null)));
        return result;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
    }
}
