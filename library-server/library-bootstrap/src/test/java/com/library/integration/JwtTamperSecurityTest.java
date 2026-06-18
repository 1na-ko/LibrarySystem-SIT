package com.library.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 10.15 JWT 篡改防护安全测试.
 * <p>
 * 修改 Token payload 后请求应 401（签名校验失败）。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@DisplayName("10.15 JWT 篡改防护")
class JwtTamperSecurityTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("篡改Token签名后请求应401")
    void shouldReturn401WhenTokenSignatureTampered() {
        String token = loginHelper.login("test_student", "Test@123456");
        // 破坏签名：替换末尾若干字符
        String tampered = token.substring(0, token.length() - 6) + "AAAAAA";
        ResponseEntity<Map> resp = restTemplate.exchange(
                API + "/users/me", HttpMethod.GET, loginHelper.auth(tampered), Map.class);
        assertThat(resp.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    @DisplayName("篡改Token payload角色后请求应401")
    void shouldReturn401WhenTokenPayloadRoleTampered() {
        String token = loginHelper.login("test_student", "Test@123456");
        // 解码 payload，修改 role，重组（签名失效）
        String[] parts = token.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
        String tamperedPayload = payload.replace("STUDENT", "ADMIN");
        parts[1] = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(tamperedPayload.getBytes());
        String tamperedToken = String.join(".", parts);
        ResponseEntity<Map> resp = restTemplate.exchange(
                API + "/admin/users?page=1&size=20", HttpMethod.GET, loginHelper.auth(tamperedToken), Map.class);
        // 签名失效 → 401（而非 403，因认证未通过）
        assertThat(resp.getStatusCode().value()).isEqualTo(401);
    }
}
