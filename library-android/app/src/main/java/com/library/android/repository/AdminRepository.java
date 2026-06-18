package com.library.android.repository;

import com.library.android.model.*;
import com.library.android.network.LibraryApi;

import io.reactivex.rxjava3.core.Single;

/**
 * 系统管理 Repository（人员 B 主导）.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public class AdminRepository {

    private final LibraryApi api;

    public AdminRepository(LibraryApi api) {
        this.api = api;
    }

    /** 用户列表（分页+筛选）. */
    public Single<Result<PageResult<UserManageVO>>> listUsers(String role, String status, String keyword,
                                                               int page, int size) {
        return Single.fromCallable(() ->
                api.listUsers(role, status, keyword, page, size).execute().body());
    }

    /** 变更用户状态. */
    public Single<Result<Void>> updateUserStatus(long userId, String status) {
        return Single.fromCallable(() ->
                api.updateUserStatus(userId, new UserStatusUpdateRequest(status)).execute().body());
    }

    /** 新增图书（编目）. */
    public Single<Result<BookVO>> createBook(BookCreateRequest request) {
        return Single.fromCallable(() ->
                api.createBook(request).execute().body());
    }

    /** 修改图书信息. */
    public Single<Result<BookVO>> updateBook(long bookId, BookUpdateRequest request) {
        return Single.fromCallable(() ->
                api.updateBook(bookId, request).execute().body());
    }

    /** 删除图书. */
    public Single<Result<Void>> deleteBook(long bookId) {
        return Single.fromCallable(() ->
                api.deleteBook(bookId).execute().body());
    }
}
