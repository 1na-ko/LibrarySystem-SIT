package com.library.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 10.1 认证全流程集成测试（P0）.
 * <p>
 * 注册→登录→认证请求→Token 刷新→登出→旧 Token 失效。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@DisplayName("10.1 认证全流程")
class AuthFlowIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("注册→登录→认证请求→刷新→登出→旧Token失效 全流程")
    void shouldCompleteAuthFlowWhenCredentialsValid() {
        // 1. 注册新用户
        Map<String, String> registerReq = Map.of(
                "username", "authflow_user",
                "password", "Test@123456",
                "realName", "认证流程测试",
                "email", "authflow@test.edu.cn",
                "phone", "13800000099");
        ResponseEntity<Map> regResp = restTemplate.postForEntity(API + "/auth/register", registerReq, Map.class);
        assertThat(regResp.getStatusCode().is2xxSuccessful()).isTrue();

        // 2. 登录拿 Token 对
        Map<?, ?> loginData = loginHelper.loginData("authflow_user", "Test@123456");
        String accessToken = (String) loginData.get("accessToken");
        String refreshToken = (String) loginData.get("refreshToken");
        assertThat(accessToken).isNotBlank();
        assertThat(refreshToken).isNotBlank();

        // 3. 带 Token 访问 /users/me
        ResponseEntity<Map> meResp = restTemplate.exchange(
                API + "/users/me", HttpMethod.GET, loginHelper.auth(accessToken), Map.class);
        assertThat(meResp.getStatusCode().is2xxSuccessful()).isTrue();

        // 4. Token 刷新
        Map<String, String> refreshReq = Map.of("refreshToken", refreshToken);
        ResponseEntity<Map> refreshResp = restTemplate.postForEntity(API + "/auth/refresh", refreshReq, Map.class);
        assertThat(refreshResp.getStatusCode().is2xxSuccessful()).isTrue();

        // 5. 登出
        ResponseEntity<Map> logoutResp = restTemplate.postForEntity(
                API + "/auth/logout", loginHelper.auth(accessToken), Map.class);
        assertThat(logoutResp.getStatusCode().is2xxSuccessful()).isTrue();

        // 6. 旧 accessToken 访问应 401（登出后失效）
        ResponseEntity<Map> afterLogout = restTemplate.exchange(
                API + "/users/me", HttpMethod.GET, loginHelper.auth(accessToken), Map.class);
        assertThat(afterLogout.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    @DisplayName("错误密码登录应返回 4xx")
    void shouldRejectLoginWhenPasswordWrong() {
        Map<String, String> req = Map.of("username", "test_student", "password", "Wrong@123456");
        ResponseEntity<Map> resp = restTemplate.postForEntity(API + "/auth/login", req, Map.class);
        assertThat(resp.getStatusCode().is4xxClientError()).isTrue();
    }
}
