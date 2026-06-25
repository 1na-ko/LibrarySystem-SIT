package com.library.android.repository;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.library.android.model.BookRecommendVO;
import com.library.android.model.BorrowRecordVO;
import com.library.android.model.BorrowStatsVO;
import com.library.android.model.PageResult;
import com.library.android.model.Result;
import com.library.android.model.UserProfile;
import com.library.android.network.ApiCallExecutor;
import com.library.android.network.LibraryApi;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.ResponseBody;
import okio.BufferedSource;
import retrofit2.Response;

/**
 * 用户中心 Repository — 个人信息、借阅历史、统计、推荐.
 * <p>
 * {@link #streamRecommendations} 提供 SSE 流式推荐接收（书目秒回 + LLM 导语逐 token），
 * 内部按标准 SSE 帧解析（event:/data: 行，空行分隔），通过 {@link RecommendStreamCallback} 回调.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public class UserRepository {

    private static final String TAG = "UserRepository";
    private static final Type BOOK_LIST_TYPE = new TypeToken<List<BookRecommendVO>>() {}.getType();

    private final LibraryApi api;
    /** 流式 SSE 手动解析用的 Gson（注册 Utf8FixTypeAdapterFactory，与 Retrofit 全局 Gson 一致，避免中文乱码）. */
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(new com.library.android.network.Utf8FixTypeAdapterFactory())
            .create();
    private final CompositeDisposable streamDisposables = new CompositeDisposable();

    public UserRepository(LibraryApi api) {
        this.api = api;
    }

    public Single<Result<UserProfile>> getMyProfile() {
        return Single.fromCallable(() -> ApiCallExecutor.execute(api.getMyProfile()));
    }

    /** 后端返回 Result&lt;Void&gt;，更新成功后需重新调用 getMyProfile() 刷新. */
    public Single<Result<Void>> updateMyProfile(Map<String, String> body) {
        return Single.fromCallable(() -> ApiCallExecutor.execute(api.updateMyProfile(body)));
    }

    public Single<Result<PageResult<BorrowRecordVO>>> getMyHistory(Integer year, int pageNum, int pageSize) {
        return Single.fromCallable(() -> ApiCallExecutor.execute(api.getMyHistory(year, pageNum, pageSize)));
    }

    public Single<Result<BorrowStatsVO>> getMyStats() {
        return Single.fromCallable(() -> ApiCallExecutor.execute(api.getMyStats()));
    }

    public Single<Result<List<BookRecommendVO>>> getRecommendations(int limit) {
        return Single.fromCallable(() -> ApiCallExecutor.execute(api.getRecommendations(limit)));
    }

    /**
     * SSE 流式推荐回调.
     */
    public interface RecommendStreamCallback {
        /** 收到 books 事件（书目列表，秒回）. */
        void onBooks(List<BookRecommendVO> books);
        /** 收到 reason 事件（LLM 导语增量 token，可能多次）. */
        void onReasonToken(String token);
        /** 流式正常结束（done 事件或流读完）. */
        void onDone();
        /** 流式失败. */
        void onError(Throwable e);
    }

    /**
     * SSE 流式接收推荐：书目秒回 + LLM 导语逐 token.
     * <p>
     * 在 IO 线程同步读取 ResponseBody，按标准 SSE 帧解析。回调在 IO 线程触发，
     * 调用方（ViewModel）应使用 {@code postValue} 更新 LiveData 以保证线程安全.
     *
     * @param limit    推荐条数上限
     * @param callback 流式回调
     */
    public void streamRecommendations(int limit, RecommendStreamCallback callback) {
        Disposable d = Single.<Boolean>fromCallable(() -> {
                    Response<ResponseBody> resp = api.streamRecommendations(limit).execute();
                    if (!resp.isSuccessful() || resp.body() == null) {
                        throw new IOException("SSE 流失败: HTTP " + resp.code());
                    }
                    try (ResponseBody body = resp.body()) {
                        parseSseStream(body, callback);
                    }
                    return true;
                })
                .subscribeOn(Schedulers.io())
                .subscribe(
                        ok -> callback.onDone(),
                        callback::onError);
        streamDisposables.add(d);
    }

    /** 取消所有进行中的流式订阅（Fragment 销毁时调用）. */
    public void disposeStreams() {
        streamDisposables.clear();
    }

    /**
     * 按标准 SSE 帧解析流：event:/data: 行，空行分隔一个事件.
     */
    private void parseSseStream(ResponseBody body, RecommendStreamCallback callback) throws IOException {
        BufferedSource source = body.source();
        String event = "message";
        StringBuilder dataBuf = new StringBuilder();
        while (!source.exhausted()) {
            String line = source.readUtf8Line();
            if (line == null) break;
            if (line.isEmpty()) {
                // 空行 = 事件边界，派发累积的 event
                if (dataBuf.length() > 0) {
                    dispatchEvent(event, dataBuf.toString(), callback);
                    dataBuf.setLength(0);
                }
                event = "message";
            } else if (line.startsWith("event:")) {
                event = line.substring(6).trim();
            } else if (line.startsWith("data:")) {
                String d = line.substring(5);
                if (d.startsWith(" ")) d = d.substring(1);
                if (dataBuf.length() > 0) dataBuf.append('\n');
                dataBuf.append(d);
            }
            // 其他行（如注释 ":comment"）忽略
        }
        // 流末尾若有余留数据，派发
        if (dataBuf.length() > 0) {
            dispatchEvent(event, dataBuf.toString(), callback);
        }
    }

    private void dispatchEvent(String event, String data, RecommendStreamCallback callback) {
        try {
            switch (event) {
                case "books":
                    List<BookRecommendVO> books = gson.fromJson(data, BOOK_LIST_TYPE);
                    callback.onBooks(books != null ? books : java.util.Collections.emptyList());
                    break;
                case "reason":
                    // reason data 是纯文本 token（可能含中文），原样回调
                    callback.onReasonToken(data);
                    break;
                case "done":
                    // done 事件：流式标记结束（onDone 由 Single 成功统一触发，此处不重复）
                    break;
                default:
                    Log.d(TAG, "未知 SSE 事件: " + event);
            }
        } catch (Exception e) {
            Log.w(TAG, "SSE 事件解析失败: event=" + event, e);
        }
    }
}
