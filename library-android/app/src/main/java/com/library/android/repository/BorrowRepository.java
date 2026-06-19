package com.library.android.repository;

import com.library.android.model.*;
import com.library.android.network.ApiCallExecutor;
import com.library.android.network.LibraryApi;

import io.reactivex.rxjava3.core.Single;

/**
 * 借阅管理 Repository.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public class BorrowRepository {

    private final LibraryApi api;

    public BorrowRepository(LibraryApi api) {
        this.api = api;
    }

    /** 借书申请. */
    public Single<Result<BorrowResultVO>> borrowBook(long bookId) {
        return Single.fromCallable(() ->
                ApiCallExecutor.execute(api.borrowBook(new BorrowRequest(bookId))));
    }

    /** 我的借阅列表（支持状态筛选+分页）. */
    public Single<Result<PageResult<BorrowRecordVO>>> getMyBorrows(String status, int pageNum, int pageSize) {
        return Single.fromCallable(() ->
                ApiCallExecutor.execute(api.getMyBorrows(status, pageNum, pageSize)));
    }

    /** 借阅详情. */
    public Single<Result<BorrowRecordVO>> getBorrowDetail(long borrowId) {
        return Single.fromCallable(() ->
                ApiCallExecutor.execute(api.getBorrowDetail(borrowId)));
    }

    /** 归还图书（后端返回 BorrowRecordVO 含归还详情）. */
    public Single<Result<BorrowRecordVO>> returnBook(long borrowId) {
        return Single.fromCallable(() ->
                ApiCallExecutor.execute(api.returnBook(borrowId)));
    }

    /** 续借图书. */
    public Single<Result<RenewResultVO>> renewBook(long borrowId) {
        return Single.fromCallable(() ->
                ApiCallExecutor.execute(api.renewBook(borrowId)));
    }

    /** 超期未还记录（管理员）. */
    public Single<Result<PageResult<BorrowRecordVO>>> getOverdueRecords(int pageNum, int pageSize) {
        return Single.fromCallable(() ->
                ApiCallExecutor.execute(api.getOverdueRecords(pageNum, pageSize)));
    }
}
