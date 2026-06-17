package com.library.integration;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.util.Map;

/**
 * 集成测试登录辅助工具.
 * <p>
 * 封装登录拿 Token 与构造认证请求头，供所有集成测试复用（DRY）。
 * V100 种子用户密码统一 Test@123456，复用 V4 admin（Admin@123456）。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class LoginHelper {

    private final TestRestTemplate restTemplate;

    /**
     * 登录并返回 accessToken（便捷方法，多数测试用）.
     */
    public String login(String username, String password) {
        return (String) loginData(username, password).get("accessToken");
    }

    /**
     * 登录并返回完整 data（含 accessToken + refreshToken，刷新流程测试用）.
     */
    public Map<?, ?> loginData(String username, String password) {
        Map<String, String> req = Map.of("username", username, "password", password);
        ResponseEntity<Map> resp = restTemplate.postForEntity("/api/v1/auth/login", req, Map.class);
        Map<?, ?> body = resp.getBody();
        if (body == null || body.get("data") == null) {
            throw new IllegalStateException("登录失败: " + username + " -> " + resp.getStatusCode());
        }
        return (Map<?, ?>) body.get("data");
    }

    /**
     * 构造带 Bearer Token 的 GET 请求实体.
     */
    public HttpEntity<Void> auth(String token) {
        return new HttpEntity<>(authHeaders(token));
    }

    /**
     * 构造带 Bearer Token 与请求体的请求实体.
     */
    public HttpEntity<?> auth(String token, Object body) {
        return new HttpEntity<>(body, authHeaders(token));
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }
}
