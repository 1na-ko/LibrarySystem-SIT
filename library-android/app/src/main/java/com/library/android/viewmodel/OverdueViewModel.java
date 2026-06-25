package com.library.android.viewmodel;

import android.util.Log;

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
 * 超期管理 ViewModel（WP2.2 新建）— 封装超期记录加载与分页.
 *
 * <p>替代 OverdueFragment 直接注入 BorrowRepository 的违规模式.
 *
 * @author LibrarySystem Team
 * @since 2.0.0
 */
@HiltViewModel
public class OverdueViewModel extends BaseViewModel {

    private static final String TAG = "OverdueViewModel";
    private static final int PAGE_SIZE = 20;

    private final BorrowRepository borrowRepository;

    private final MutableLiveData<List<BorrowRecordVO>> overdueList = new MutableLiveData<>(new ArrayList<>());

    private int currentPage = 0;
    private int totalPages = 0;
    private boolean isLoadingMore = false;

    @Inject
    public OverdueViewModel(BorrowRepository borrowRepository) {
        this.borrowRepository = borrowRepository;
    }

    public LiveData<List<BorrowRecordVO>> getOverdueList() {
        return overdueList;
    }

    /** 加载第一页（重置分页状态）. */
    public void loadFirstPage() {
        setLoading(LoadingState.LOADING);
        currentPage = 1;
        isLoadingMore = false;

        disposables.add(borrowRepository.getOverdueRecords(currentPage, PAGE_SIZE)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> handleFirstPageResult(result),
                        this::postError
                ));
    }

    /** 加载下一页（追加模式）. */
    public void loadNextPage() {
        if (isLoadingMore || currentPage >= totalPages) return;
        isLoadingMore = true;
        currentPage++;

        disposables.add(borrowRepository.getOverdueRecords(currentPage, PAGE_SIZE)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> handleNextPageResult(result),
                        throwable -> {
                            isLoadingMore = false;
                            currentPage--;  // 回退页码
                            postError(throwable);
                        }
                ));
    }

    public boolean hasMore() {
        return currentPage < totalPages && !isLoadingMore;
    }

    private void handleFirstPageResult(Result<PageResult<BorrowRecordVO>> result) {
        if (result.isSuccess() && result.getData() != null) {
            List<BorrowRecordVO> records = result.getData().getRecords();
            totalPages = result.getData().getTotalPages();
            overdueList.setValue(records != null ? records : new ArrayList<>());
            setLoading(records != null && !records.isEmpty() ? LoadingState.CONTENT : LoadingState.EMPTY);
        } else {
            setLoading(LoadingState.ERROR);
            postError(new RuntimeException(result.getMessage()));
        }
    }

    private void handleNextPageResult(Result<PageResult<BorrowRecordVO>> result) {
        isLoadingMore = false;
        if (result.isSuccess() && result.getData() != null) {
            List<BorrowRecordVO> newRecords = result.getData().getRecords();
            if (newRecords != null && !newRecords.isEmpty()) {
                List<BorrowRecordVO> current = new ArrayList<>(overdueList.getValue() != null
                        ? overdueList.getValue() : new ArrayList<>());
                current.addAll(newRecords);
                overdueList.setValue(current);
            }
        } else {
            // 加载失败，回退页码
            currentPage--;
        }
    }
}
