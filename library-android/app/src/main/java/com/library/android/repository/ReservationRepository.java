package com.library.android.repository;

import com.library.android.model.*;
import com.library.android.network.LibraryApi;

import io.reactivex.rxjava3.core.Single;

/**
 * 预约管理 Repository（人员 B 主导）.
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
                api.reserveBook(new ReservationRequest(bookId)).execute().body());
    }

    /** 我的预约列表（支持状态筛选+分页）. */
    public Single<Result<PageResult<ReservationVO>>> getMyReservations(String status, int pageNum, int pageSize) {
        return Single.fromCallable(() ->
                api.getMyReservations(status, pageNum, pageSize).execute().body());
    }

    /** 取消预约. */
    public Single<Result<Void>> cancelReservation(long reservationId) {
        return Single.fromCallable(() ->
                api.cancelReservation(reservationId).execute().body());
    }

    /** 查询排队位置. */
    public Single<Result<QueuePositionVO>> getQueuePosition(long reservationId) {
        return Single.fromCallable(() ->
                api.getQueuePosition(reservationId).execute().body());
    }
}
