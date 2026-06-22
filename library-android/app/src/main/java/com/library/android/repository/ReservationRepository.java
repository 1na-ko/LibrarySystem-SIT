package com.library.android.repository;

import com.library.android.model.*;
import com.library.android.network.ApiCallExecutor;
import com.library.android.network.LibraryApi;

import io.reactivex.rxjava3.core.Single;

/**
 * 预约管理 Repository.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public class ReservationRepository {

    private final LibraryApi api;

    public ReservationRepository(LibraryApi api) {
        this.api = api;
    }

    /** 预约图书. */
    public Single<Result<ReservationVO>> reserveBook(long bookId) {
        return Single.fromCallable(() ->
                ApiCallExecutor.execute(api.reserveBook(new ReservationRequest(bookId))));
    }

    /** 我的预约列表（支持状态筛选+分页）. */
    public Single<Result<PageResult<ReservationVO>>> getMyReservations(String status, int pageNum, int pageSize) {
        return Single.fromCallable(() ->
                ApiCallExecutor.execute(api.getMyReservations(status, pageNum, pageSize)));
    }

    /** 取消预约. */
    public Single<Result<Void>> cancelReservation(long reservationId) {
        return Single.fromCallable(() ->
                ApiCallExecutor.execute(api.cancelReservation(reservationId)));
    }

    /** 查询排队位置（后端契约：Result&lt;Integer&gt; 仅排队序号）. */
    public Single<Result<Integer>> getQueuePosition(long reservationId) {
        return Single.fromCallable(() ->
                ApiCallExecutor.execute(api.getQueuePosition(reservationId)));
    }
}
