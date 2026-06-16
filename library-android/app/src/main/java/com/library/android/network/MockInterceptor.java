package com.library.android.network;

import android.util.Log;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * 模拟拦截器 — 不依赖后端即可测试登录流程.
 *
 * <p>拦截 POST /auth/login 请求，直接返回伪造的成功响应，
 * 使前端完整的登录流程（ViewModel → TokenManager → 跳转）可独立验证.
 */
public class MockInterceptor implements Interceptor {

    private static final String TAG = "MockInterceptor";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    /** 是否启用模拟模式，设为 false 则放行所有请求到真实后端 */
    private boolean enabled = true;

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        String path = chain.request().url().encodedPath();
        String method = chain.request().method();

        if (!enabled) {
            return chain.proceed(chain.request());
        }

        // === 模拟登录接口 ===
        if (method.equals("POST") && path.contains("/auth/login")) {
            Log.d(TAG, "Mock → 拦截登录请求，返回伪造成功响应");

            String fakeResponse = "{"
                    + "\"code\": 200,"
                    + "\"message\": \"登录成功\","
                    + "\"data\": {"
                    + "  \"accessToken\": \"mock_access_token_abc123\","
                    + "  \"refreshToken\": \"mock_refresh_token_xyz789\","
                    + "  \"tokenType\": \"Bearer\","
                    + "  \"expiresIn\": 7200,"
                    + "  \"user\": {"
                    + "    \"id\": 1,"
                    + "    \"username\": \"2024001001\","
                    + "    \"realName\": \"模拟用户\","
                    + "    \"role\": \"STUDENT\","
                    + "    \"email\": \"mock@university.edu.cn\","
                    + "    \"status\": \"ACTIVE\""
                    + "  }"
                    + "},"
                    + "\"timestamp\": " + System.currentTimeMillis()
                    + "}";

            return new Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(ResponseBody.create(fakeResponse, JSON))
                    .build();
        }

        // === 模拟 Token 刷新接口 ===
        if (method.equals("POST") && path.contains("/auth/refresh")) {
            Log.d(TAG, "Mock → 拦截刷新请求，返回新 Token");

            String fakeRefresh = "{"
                    + "\"code\": 200,"
                    + "\"message\": \"刷新成功\","
                    + "\"data\": {"
                    + "  \"accessToken\": \"mock_new_access_token_456\","
                    + "  \"refreshToken\": \"mock_new_refresh_token_012\","
                    + "  \"tokenType\": \"Bearer\","
                    + "  \"expiresIn\": 7200"
                    + "},"
                    + "\"timestamp\": " + System.currentTimeMillis()
                    + "}";

            return new Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(ResponseBody.create(fakeRefresh, JSON))
                    .build();
        }

        // 其他请求放行到真实后端
        return chain.proceed(chain.request());
    }
}