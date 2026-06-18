package com.library.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 10.14 XSS 防护安全测试.
 * <p>
 * V100 种子 10009 书名含 {@code <script>alert(1)</script>}，验证 JSON API 输出不执行脚本
 * （Content-Type=application/json，浏览器不解析；title 作为字符串值安全）。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@DisplayName("10.14 XSS 防护")
class XssSecurityTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("书名含script标签应作为字符串安全输出不破坏JSON")
    void shouldOutputScriptTagAsStringWhenBookTitleContainsXss() {
        String token = loginHelper.login("test_student", "Test@123456");
        // V100 种子 10009 书名: <script>alert(1)</script>测试书
        ResponseEntity<Map> resp = restTemplate.exchange(
                API + "/books/10009", HttpMethod.GET, loginHelper.auth(token), Map.class);
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();

        // 响应 Content-Type 应为 JSON（浏览器不执行其中脚本）
        MediaType contentType = resp.getHeaders().getContentType();
        assertThat(contentType).isNotNull();
        assertThat(contentType.isCompatibleWith(MediaType.APPLICATION_JSON)).isTrue();

        // title 含 script 标签但作为合法 JSON 字符串值（已成功反序列化为 Map）
        Map<?, ?> data = (Map<?, ?>) resp.getBody().get("data");
        assertThat(data.get("title").toString()).contains("script");
    }
}
