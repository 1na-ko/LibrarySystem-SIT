package com.library.android.repository;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.library.android.model.DuplicateCheckResult;
import com.library.android.model.ElectronicResourceVO;
import com.library.android.model.GapAnalysisResult;
import com.library.android.model.NegotiationSuggestion;
import com.library.android.model.NegotiationVO;
import com.library.android.model.PurchasePredictionVO;
import com.library.android.model.Result;
import com.library.android.model.SupplierVO;
import com.library.android.network.ApiCallExecutor;
import com.library.android.network.LibraryApi;
import com.library.android.network.Utf8FixTypeAdapterFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
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
 * 智能采编 Repository — 采购预测 / 查重 / 缺口分析 / 谈判（含 WP6 SSE 流式）.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public class AcquisitionRepository {

    private static final String TAG = "AcquisitionRepository";
    private final LibraryApi api;
    /** SSE 解析用 Gson（注册 Utf8Fix 防中文乱码，与全局 Retrofit Gson 一致）. */
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(new Utf8FixTypeAdapterFactory())
            .create();
    private final CompositeDisposable streamDisposables = new CompositeDisposable();
    /** 流式取消标志 — 合作取消 OkHttp 阻塞读取，防止 Fragment 销毁后回调触发异常. */
    private volatile boolean streamCancelled = false;

    public AcquisitionRepository(LibraryApi api) {
        this.api = api;
    }

    /** 采购需求预测（按学科分类，预测未来 N 个月）. */
    public Single<Result<List<PurchasePredictionVO>>> predictDemand(long subjectId, int months) {
        return Single.fromCallable(() ->
                ApiCallExecutor.execute(api.predictDemand(subjectId, months)));
    }

    /** 图书查重（按 ISBN/标题/作者三策略）. */
    public Single<Result<DuplicateCheckResult>> checkDuplicate(String isbn, String title, String author) {
        Map<String, String> body = new HashMap<>();
        if (isbn != null && !isbn.isEmpty()) body.put("isbn", isbn);
        if (title != null && !title.isEmpty()) body.put("title", title);
        if (author != null && !author.isEmpty()) body.put("author", author);
        return Single.fromCallable(() ->
                ApiCallExecutor.execute(api.checkDuplicate(body)));
    }

    /** 学科缺口分析. */
    public Single<Result<GapAnalysisResult>> analyzeGap(long subjectId) {
        return Single.fromCallable(() ->
                ApiCallExecutor.execute(api.analyzeGap(subjectId)));
    }

    /** 创建谈判会话（negotiatorId 由后端 SecurityUtils 自动填充）. */
    public Single<Result<NegotiationVO>> createNegotiation(long resourceId, long supplierId) {
        return Single.fromCallable(() ->
                ApiCallExecutor.execute(api.createNegotiation(resourceId, supplierId)));
    }

    /** 获取谈判 LLM 建议（同步，含 priceRange / strategies / keyTerms / riskWarnings）. */
    public Single<Result<NegotiationSuggestion>> getNegotiationSuggestion(long negotiationId) {
        return Single.fromCallable(() ->
                ApiCallExecutor.execute(api.getNegotiationSuggestion(negotiationId)));
    }

    /** WP5：供应商列表（下拉选择用）. */
    public Single<Result<List<SupplierVO>>> listSuppliers() {
        return Single.fromCallable(() -> ApiCallExecutor.execute(api.listSuppliers()));
    }

    /** WP5：电子资源列表（下拉选择用）. */
    public Single<Result<List<ElectronicResourceVO>>> listResources() {
        return Single.fromCallable(() -> ApiCallExecutor.execute(api.listResources()));
    }

    // ======================== WP6: 谈判建议 SSE 流式 ========================

    /** SSE 流式回调（WP2.6：PriceRange 统一使用 NegotiationSuggestion.PriceRange）. */
    public interface NegotiationStreamCallback {
        void onPriceRange(NegotiationSuggestion.PriceRange range);
        void onTextToken(String token);
        void onDone();
        void onError(Throwable e);
    }

    /**
     * 流式接收谈判建议（priceRange 秒回 + text 多次逐 token + done）.
     * <p>回调在 IO 线程触发，调用方应用 LiveData.postValue 保证线程安全.
     */
    public void streamNegotiationSuggestion(long negotiationId, NegotiationStreamCallback callback) {
        streamCancelled = false;  // 新流启动时重置标志
        Disposable d = Single.<Boolean>fromCallable(() -> {
                    Response<ResponseBody> resp = api.streamNegotiationSuggestion(negotiationId).execute();
                    if (!resp.isSuccessful() || resp.body() == null) {
                        throw new IOException("SSE 流失败: HTTP " + resp.code());
                    }
                    boolean completed = false;
                    try (ResponseBody body = resp.body()) {
                        completed = parseSseStream(body, callback);
                    } catch (IOException e) {
                        // done 后服务端关闭连接时 body.close() 可能抛 IOException，流已完成则吞掉
                        if (!completed) throw e;
                        Log.d(TAG, "SSE 流正常结束，忽略 close 异常: " + e.getMessage());
                    }
                    return true;
                })
                .subscribeOn(Schedulers.io())
                .subscribe(
                    ok -> { if (!streamCancelled) callback.onDone(); },
                    e -> { if (!streamCancelled) callback.onError(e); }
                );
        streamDisposables.add(d);
    }

    public void disposeStreams() {
        streamCancelled = true;
        streamDisposables.clear();
    }

    /** 按标准 SSE 帧解析：event:/data: 行，空行分隔事件.
     *  @return true 表示收到 done 事件正常完成 */
    private boolean parseSseStream(ResponseBody body, NegotiationStreamCallback callback) throws IOException {
        BufferedSource source = body.source();
        String event = "message";
        StringBuilder dataBuf = new StringBuilder();
        while (!source.exhausted()) {
            if (streamCancelled) {
                callback.onError(new IOException("Stream cancelled"));
                return false;
            }
            String line = source.readUtf8Line();
            if (line == null) break;
            if (line.isEmpty()) {
                // "done" 事件 data 可能为空，不检查 dataBuf 长度直接处理
                if ("done".equals(event)) {
                    dispatchEvent(event, "", callback);
                    return true;
                }
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
        }
        if (dataBuf.length() > 0) {
            dispatchEvent(event, dataBuf.toString(), callback);
        }
        return false;  // 未收到 done 事件（流自然结束或中断）
    }

    private void dispatchEvent(String event, String data, NegotiationStreamCallback callback) {
        try {
            switch (event) {
                case "priceRange":
                    NegotiationSuggestion.PriceRange pr = gson.fromJson(data, NegotiationSuggestion.PriceRange.class);
                    if (pr != null) callback.onPriceRange(pr);
                    break;
                case "text":
                    callback.onTextToken(data);
                    break;
                case "done":
                    // done 事件由 Single onSuccess 统一触发，此处不重复；但数据已完整接收，不需要再读后续行
                    break;
                default:
                    Log.d(TAG, "未知 SSE 事件: " + event);
            }
        } catch (Exception e) {
            Log.w(TAG, "SSE 事件解析失败: event=" + event, e);
        }
    }
}
