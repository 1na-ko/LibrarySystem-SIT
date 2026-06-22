package com.library.android.network;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.library.android.model.Result;
import com.library.android.network.exception.ApiException;
import com.library.android.network.exception.BizConflictException;
import com.library.android.network.exception.NetworkException;
import com.library.android.network.exception.NotFoundException;
import com.library.android.network.exception.PermissionDeniedException;
import com.library.android.network.exception.RateLimitException;
import com.library.android.network.exception.ServiceUnavailableException;
import com.library.android.network.exception.SessionExpiredException;
import com.library.android.network.exception.ValidationException;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Retrofit Call 同步执行器 — 统一翻译 HTTP/IO 异常为业务异常.
 *
 * <p>所有 Repository 应通过此工具调用 Retrofit 接口，禁止裸 {@code call.execute().body()}.
 * 翻译规则:
 * <ul>
 *   <li>HTTP 200 + body 非空 → 返回 body（业务码由 ViewModel 检查 {@link Result#isSuccess()}）</li>
 *   <li>HTTP 401 → {@link SessionExpiredException}（Authenticator 已尝试 refresh 但失败）</li>
 *   <li>HTTP 403 → {@link PermissionDeniedException}</li>
 *   <li>HTTP 409 → {@link BizConflictException}（携带后端 message）</li>
 *   <li>HTTP 400/422 → {@link ValidationException}</li>
 *   <li>HTTP 502/503/504 → {@link ServiceUnavailableException}</li>
 *   <li>其他非 2xx → {@link ApiException}</li>
 *   <li>{@link IOException} → {@link NetworkException}</li>
 * </ul>
 *
 * <p>HTTP 200 但 errorBody 不为 null 的情况：保留 body 不抛异常，由 ViewModel 通过
 * {@code result.isSuccess()} + {@code result.getMessage()} 处理（兼容现有逻辑）.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public final class ApiCallExecutor {

    private ApiCallExecutor() {
        // 工具类禁止实例化
    }

    /**
     * 同步执行 Retrofit Call 并按 HTTP 状态码翻译异常.
     *
     * @param call Retrofit 同步调用
     * @param <T>  Result 包装的业务数据类型
     * @return 200 时返回的 Result 对象（非空）
     * @throws ApiException     按 HTTP 状态码细分的业务异常
     * @throws NetworkException 网络/IO 失败
     */
    @NonNull
    public static <T> Result<T> execute(@NonNull Call<Result<T>> call) {
        Response<Result<T>> response;
        try {
            response = call.execute();
        } catch (IOException e) {
            throw new NetworkException(e);
        }

        int code = response.code();
        if (response.isSuccessful() && response.body() != null) {
            return response.body();
        }

        String serverMessage = parseErrorMessage(response);
        switch (code) {
            case 401:
                throw new SessionExpiredException(serverMessage);
            case 403:
                throw new PermissionDeniedException(serverMessage);
            case 404:
                // P2-07：明确区分 NotFound，UI 可展示更友好的"资源不存在"文案
                throw new NotFoundException(serverMessage);
            case 409:
                throw new BizConflictException(serverMessage);
            case 429:
                // P2-07：限流异常，附 Retry-After 建议（后端 RateLimitFilter 触发）
                int retryAfter = parseRetryAfter(response.headers().get("Retry-After"));
                throw new RateLimitException(serverMessage, retryAfter);
            case 400:
            case 422:
                throw new ValidationException(code, serverMessage);
            case 502:
            case 503:
            case 504:
                throw new ServiceUnavailableException(code, serverMessage);
            default:
                throw new ApiException(code, serverMessage);
        }
    }

    /** 解析 Retry-After 头部（秒数），缺失或非法返回 0. */
    private static int parseRetryAfter(@androidx.annotation.Nullable String value) {
        if (value == null || value.isEmpty()) return 0;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignore) {
            return 0;
        }
    }

    /**
     * 尝试从 errorBody 解析 Result.message 字段.
     *
     * <p>后端 GlobalExceptionHandler 错误响应也走 Result 结构（@JsonInclude(NON_NULL)
     * 会去掉 data null），所以可以按 JsonObject 取 message.
     */
    @androidx.annotation.Nullable
    private static String parseErrorMessage(Response<?> response) {
        ResponseBody errorBody = response.errorBody();
        if (errorBody == null) {
            return null;
        }
        try {
            String json = errorBody.string();
            if (json == null || json.isEmpty()) {
                return null;
            }
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            if (obj.has("message") && !obj.get("message").isJsonNull()) {
                return obj.get("message").getAsString();
            }
            return null;
        } catch (Exception ignore) {
            // errorBody 已被消费或非 JSON 格式，返回 null 由调用方降级
            return null;
        } finally {
            errorBody.close();
        }
    }
}
