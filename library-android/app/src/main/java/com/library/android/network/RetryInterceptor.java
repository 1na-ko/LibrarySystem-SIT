package com.library.android.network;

import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 网络重试拦截器（P2-07）— 对幂等 GET / HEAD 请求的瞬时网络异常重试 1 次.
 *
 * <p>设计原则：
 * <ul>
 *   <li>仅对 GET / HEAD 重试（POST/PUT/DELETE 可能产生副作用，禁止重试）</li>
 *   <li>仅对 {@link IOException} 重试（HTTP 5xx 不重试 — 服务端能感知失败由业务侧处理）</li>
 *   <li>不重试 {@link InterruptedIOException}（用户主动取消 / 流式响应等）</li>
 *   <li>固定重试 1 次，固定回退 800ms（更激进的指数退避在客户端无意义，反而拖慢失败感知）</li>
 *   <li>SocketTimeoutException 视为可重试 — 移动网络 RTT 抖动场景常见</li>
 * </ul>
 *
 * <p>放置位置：在 {@link AuthInterceptor} 之外，在 LoggingInterceptor 之前.
 *
 * @author LibrarySystem Team
 * @since 1.1.0
 */
public class RetryInterceptor implements Interceptor {

    private static final String TAG = "RetryInterceptor";
    private static final int MAX_RETRY = 1;
    private static final long RETRY_DELAY_MS = 800L;

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();
        IOException lastError = null;

        for (int attempt = 0; attempt <= MAX_RETRY; attempt++) {
            try {
                if (attempt > 0) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("retry interrupted", ie);
                    }
                    Log.i(TAG, "网络重试 #" + attempt + " " + request.method() + " " + request.url());
                }
                return chain.proceed(request);
            } catch (IOException e) {
                lastError = e;
                if (!shouldRetry(request, e, attempt)) {
                    throw e;
                }
            }
        }
        // 最大重试后仍失败
        throw lastError != null ? lastError : new IOException("retry exhausted");
    }

    private boolean shouldRetry(Request request, IOException e, int currentAttempt) {
        if (currentAttempt >= MAX_RETRY) return false;
        // 仅 GET / HEAD 幂等方法允许重试
        if (!"GET".equals(request.method()) && !"HEAD".equals(request.method())) return false;
        // 用户取消 / 流式响应 InterruptedIOException 不重试（除 SocketTimeoutException 外）
        if (e instanceof InterruptedIOException && !(e instanceof SocketTimeoutException)) return false;
        return true;
    }
}
