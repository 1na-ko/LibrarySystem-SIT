package com.library.android.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

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
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 借阅管理 ViewModel（人员 B 主导）.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@HiltViewModel
public class BorrowViewModel extends BaseViewModel {

    private final BorrowRepository repository;

    /** WP-5：删除遮蔽的 loadingState（继承自 BaseViewModel，使用 setLoading() / getLoadingState()）. */
    private final MutableLiveData<List<BorrowRecordVO>> borrowList = new MutableLiveData<>(new ArrayList<>());
    /** 最近一次借阅失败的后端 message（P2 修复：供 BorrowConfirmDialog 展示具体原因）. */
    private final MutableLiveData<String> borrowErrorMessage = new MutableLiveData<>();

    private int currentPage = 1;
    private int totalPages = 0;
    private String currentStatusFilter = null;
    private boolean isLoading = false;

    @Inject
    public BorrowViewModel(BorrowRepository repository) {
        this.repository = repository;
    }

    public LiveData<List<BorrowRecordVO>> getBorrowList() { return borrowList; }
    public LiveData<String> getBorrowErrorMessage() { return borrowErrorMessage; }

    /** 加载借阅列表（首次加载或状态切换）. */
    public void loadBorrows(String status) {
        currentStatusFilter = status;
        currentPage = 1;
        setLoading(LoadingState.LOADING);

        disposables.add(repository.getMyBorrows(status, currentPage, 20)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    if (result != null && result.isSuccess() && result.getData() != null) {
                        PageResult<BorrowRecordVO> page = result.getData();
                        List<BorrowRecordVO> records = page.getRecords();
                        borrowList.setValue(records != null ? records : new ArrayList<>());
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
                .subscribe(r -> {
                            if (r != null && r.isSuccess()) {
                                borrowErrorMessage.setValue(null);
                                result.setValue(true);
                            } else {
                                // P2 修复：保留后端具体 message（如"您已借阅该书，不可重复借阅"）
                                borrowErrorMessage.setValue(r != null ? r.getMessage() : "借阅失败");
                                result.setValue(false);
                            }
                        },
                        throwable -> {
                            // ApiException 优先取后端 serverMessage，避免 "HTTP 409:" 前缀污染提示
                            String msg = (throwable instanceof com.library.android.network.exception.ApiException)
                                    ? ((com.library.android.network.exception.ApiException) throwable).getServerMessage()
                                    : throwable.getMessage();
                            borrowErrorMessage.setValue((msg != null && !msg.isEmpty()) ? msg : "借阅失败");
                            result.setValue(false);
                        }));
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


}
