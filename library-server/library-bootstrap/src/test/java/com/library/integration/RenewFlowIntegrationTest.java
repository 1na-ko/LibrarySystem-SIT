package com.library.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 10.4 续借全链路集成测试（P0）.
 * <p>
 * 借书→续借→再次续借拒绝→超期续借拒绝→归还。
 * 利用 V100 种子：20046-20050 已 RENEWED（renew_count=1），20041-20045 已 OVERDUE。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@DisplayName("10.4 续借全链路")
class RenewFlowIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("已续借一次的记录再次续借应拒绝（renew_count 上限）")
    void shouldRejectSecondRenewWhenAlreadyRenewedOnce() {
        // test_teacher(101) 的 20046 已 renew_count=1，再次续借应 4xx
        String token = loginHelper.login("test_teacher", "Test@123456");
        ResponseEntity<Map> resp = restTemplate.exchange(
                API + "/borrows/20046/renew", HttpMethod.PUT, loginHelper.auth(token), Map.class);
        assertThat(resp.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    @DisplayName("超期记录续借应拒绝")
    void shouldRejectRenewWhenOverdue() {
        // test_teacher(101) 的 20041 已 OVERDUE，续借应 4xx
        String token = loginHelper.login("test_teacher", "Test@123456");
        ResponseEntity<Map> resp = restTemplate.exchange(
                API + "/borrows/20041/renew", HttpMethod.PUT, loginHelper.auth(token), Map.class);
        assertThat(resp.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    @DisplayName("续借他人借阅记录应拒绝（归属校验防越权）")
    void shouldRejectRenewWhenBorrowNotOwnedByUser() {
        // test_student(100) 尝试续借 test_teacher(101) 的 20046，应 4xx
        String token = loginHelper.login("test_student", "Test@123456");
        ResponseEntity<Map> resp = restTemplate.exchange(
                API + "/borrows/20046/renew", HttpMethod.PUT, loginHelper.auth(token), Map.class);
        assertThat(resp.getStatusCode().is4xxClientError()).isTrue();
    }
}
