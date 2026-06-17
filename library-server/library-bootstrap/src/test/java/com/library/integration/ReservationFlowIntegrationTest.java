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
 * 10.3 预约全流程集成测试（P0）.
 * <p>
 * 借完→预约→还书→MQ→自动通知→确认→借书→完成。
 * 归还触发 BookReturnedEvent→EventBusBridge→MQ→ReservationNotifier 通知队首读者。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@DisplayName("10.3 预约全流程")
class ReservationFlowIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("归还预约图书后应经MQ通知队首读者")
    void shouldNotifyWaiterWhenReservedBookReturned() {
        String teacherToken = loginHelper.login("test_teacher", "Test@123456");
        String studentToken = loginHelper.login("test_student", "Test@123456");

        // 1. teacher 借 10003（avail 1→0）
        Map<String, Object> borrowReq = Map.of("bookId", 10003);
        ResponseEntity<Map> borrowResp = restTemplate.postForEntity(
                API + "/borrows", loginHelper.auth(teacherToken, borrowReq), Map.class);
        Long borrowId = ((Number) ((Map<?, ?>) borrowResp.getBody().get("data")).get("borrowId")).longValue();

        // 2. student 预约 10003（avail=0 才允许预约）
        Map<String, Object> reserveReq = Map.of("bookId", 10003);
        ResponseEntity<Map> reserveResp = restTemplate.postForEntity(
                API + "/reservations", loginHelper.auth(studentToken, reserveReq), Map.class);
        assertThat(reserveResp.getStatusCode().is2xxSuccessful()).isTrue();

        // 3. teacher 还书 → BookReturnedEvent → MQ → ReservationNotifier 通知队首
        restTemplate.exchange(API + "/borrows/" + borrowId + "/return",
                HttpMethod.PUT, loginHelper.auth(teacherToken), Map.class);

        // 4. Awaitility 等 student 预约 10003 状态变 NOTIFIED（MQ 异步消费）
        await().atMost(10, SECONDS).untilAsserted(() -> {
            ResponseEntity<Map> resp = restTemplate.exchange(
                    API + "/reservations?status=NOTIFIED", HttpMethod.GET, loginHelper.auth(studentToken), Map.class);
            Map<?, ?> data = (Map<?, ?>) resp.getBody().get("data");
            List<?> records = (List<?>) data.get("records");
            boolean notified = records.stream().anyMatch(r -> {
                Map<?, ?> rec = (Map<?, ?>) r;
                Map<?, ?> book = (Map<?, ?>) rec.get("book");
                return book != null && ((Number) book.get("id")).longValue() == 10003L;
            });
            assertThat(notified).isTrue();
        });
    }
}
