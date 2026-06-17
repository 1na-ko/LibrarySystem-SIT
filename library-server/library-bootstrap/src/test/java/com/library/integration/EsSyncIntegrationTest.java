package com.library.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * 10.8 ES 同步验证集成测试（P1）.
 * <p>
 * 图书新增→ES 可搜索；修改→ES 更新；删除→ES 移除。经 MQ 事件总线（EventBusBridge→ESSyncListener）。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@DisplayName("10.8 ES 同步验证")
class EsSyncIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("创建→ES可搜；修改→ES更新；删除→ES移除")
    void shouldSyncBookChangesToEsViaMq() {
        String token = loginHelper.login("test_librarian", "Test@123456");

        // 创建
        Map<String, Object> createReq = Map.of(
                "isbn", "978-7-111-77777-7", "title", "ES同步测试原书名",
                "author", "测试", "categoryId", 101, "totalCopies", 2);
        ResponseEntity<Map> createResp = restTemplate.postForEntity(
                API + "/admin/books", loginHelper.auth(token, createReq), Map.class);
        Long bookId = asLong(((Map<?, ?>) createResp.getBody().get("data")).get("id"));

        await().atMost(10, SECONDS).untilAsserted(() ->
                assertThat(searchHasTitle(token, "ES同步测试原书名")).isTrue());

        // 修改
        Map<String, Object> updateReq = Map.of("title", "ES同步测试改后书名");
        restTemplate.exchange(API + "/admin/books/" + bookId, HttpMethod.PUT,
                loginHelper.auth(token, updateReq), Map.class);
        await().atMost(10, SECONDS).untilAsserted(() ->
                assertThat(searchHasTitle(token, "ES同步测试改后书名")).isTrue());

        // 删除
        restTemplate.exchange(API + "/admin/books/" + bookId, HttpMethod.DELETE,
                loginHelper.auth(token), Map.class);
        await().atMost(10, SECONDS).untilAsserted(() ->
                assertThat(searchHasTitle(token, "ES同步测试改后书名")).isFalse());
    }

    @SuppressWarnings("unchecked")
    private boolean searchHasTitle(String token, String title) {
        ResponseEntity<Map> s = restTemplate.exchange(
                API + "/books/search?keyword=" + title + "&pageNum=1&pageSize=20",
                HttpMethod.GET, loginHelper.auth(token), Map.class);
        Map<?, ?> data = (Map<?, ?>) s.getBody().get("data");
        List<?> records = (List<?>) data.get("records");
        return records.stream().anyMatch(r -> title.equals(((Map<?, ?>) r).get("title")));
    }
}
