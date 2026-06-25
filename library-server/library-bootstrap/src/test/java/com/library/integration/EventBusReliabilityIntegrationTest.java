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
 * 事件总线可靠性集成测试.
 * <p>
 * 验证阶段 10 引入的 RabbitMQ 事件总线端到端链路：
 * 创建图书 → BookCreatedEvent → EventBusBridge(AFTER_COMMIT) → MQ → ESSyncListener → ES 可搜索。
 * 用 Awaitility 等待 MQ 异步消费完成。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@DisplayName("事件总线可靠性（MQ 端到端）")
class EventBusReliabilityIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("创建图书后应经MQ事件总线同步至ES可搜索")
    void shouldSyncBookToEsViaEventBusWhenCreated() {
        String token = loginHelper.login("test_librarian", "Test@123456");

        // 创建图书 → 触发 BookCreatedEvent → EventBusBridge → MQ → ESSyncListener
        Map<String, Object> req = Map.of(
                "isbn", "978-7-111-88888-8",
                "title", "事件总线测试新书",
                "author", "测试作者",
                "categoryId", 101,
                "totalCopies", 3);
        ResponseEntity<Map> resp = restTemplate.postForEntity(
                API + "/admin/books", loginHelper.auth(token, req), Map.class);
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();

        // Awaitility 等待 MQ→ESSyncListener 异步同步 ES（IK 分词后可搜）
        await().atMost(10, SECONDS).untilAsserted(() -> {
            ResponseEntity<Map> searchResp = restTemplate.exchange(
                    API + "/books/search?keyword=事件总线测试新书&pageNum=1&pageSize=10",
                    HttpMethod.GET, loginHelper.auth(token), Map.class);
            List<?> records = (List<?>) ((Map<?, ?>) searchResp.getBody().get("data")).get("records");
            boolean found = records.stream().anyMatch(r ->
                    "事件总线测试新书".equals(((Map<?, ?>) r).get("title")));
            assertThat(found).isTrue();
        });
    }
}
