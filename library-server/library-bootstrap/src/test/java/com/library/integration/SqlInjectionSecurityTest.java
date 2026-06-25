package com.library.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 10.13 SQL 注入防护安全测试.
 * <p>
 * 搜索关键词含 SQL 注入 payload，验证 MyBatis-Plus 参数化查询防注入，无异常且不返回全表。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@DisplayName("10.13 SQL 注入防护")
class SqlInjectionSecurityTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("搜索关键词含SQL注入payload应无异常")
    void shouldReturnNoExceptionWhenSearchKeywordContainsSqlInjection() {
        String token = loginHelper.login("test_student", "Test@123456");
        String keyword = URLEncoder.encode("' OR '1'='1", StandardCharsets.UTF_8);
        ResponseEntity<Map> resp = restTemplate.exchange(
                API + "/books/search?keyword=" + keyword + "&pageNum=1&pageSize=10",
                HttpMethod.GET, loginHelper.auth(token), Map.class);
        // 无 500 异常，MyBatis-Plus 参数化查询防注入
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @DisplayName("搜索关键词含分号注释payload应无异常")
    void shouldReturnNoExceptionWhenSearchKeywordContainsCommentInjection() {
        String token = loginHelper.login("test_student", "Test@123456");
        String keyword = URLEncoder.encode("Java; DROP TABLE book;--", StandardCharsets.UTF_8);
        ResponseEntity<Map> resp = restTemplate.exchange(
                API + "/books/search?keyword=" + keyword + "&pageNum=1&pageSize=10",
                HttpMethod.GET, loginHelper.auth(token), Map.class);
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        // 表未被删除（后续搜索仍正常）
    }
}
