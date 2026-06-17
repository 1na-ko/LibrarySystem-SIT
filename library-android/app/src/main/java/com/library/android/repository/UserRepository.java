package com.library.android.repository;

import com.library.android.model.BookRecommendVO;
import com.library.android.model.BorrowRecordVO;
import com.library.android.model.BorrowStatsVO;
import com.library.android.model.PageResult;
import com.library.android.model.Result;
import com.library.android.model.UserProfile;
import com.library.android.network.LibraryApi;

import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.core.Single;

/**
 * 用户中心 Repository — 个人信息、借阅历史、统计、推荐.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public class UserRepository {

    private final LibraryApi api;

    public UserRepository(LibraryApi api) {
        this.api = api;
    }

    public Single<Result<UserProfile>> getMyProfile() {
        return Single.fromCallable(() -> api.getMyProfile().execute().body());
    }

    public Single<Result<UserProfile>> updateMyProfile(Map<String, String> body) {
        return Single.fromCallable(() -> api.updateMyProfile(body).execute().body());
    }

    public Single<Result<PageResult<BorrowRecordVO>>> getMyHistory(Integer year, int pageNum, int pageSize) {
        return Single.fromCallable(() -> api.getMyHistory(year, pageNum, pageSize).execute().body());
    }

    public Single<Result<BorrowStatsVO>> getMyStats() {
        return Single.fromCallable(() -> api.getMyStats().execute().body());
    }

    public Single<Result<List<BookRecommendVO>>> getRecommendations(int limit) {
        return Single.fromCallable(() -> api.getRecommendations(limit).execute().body());
    }
}