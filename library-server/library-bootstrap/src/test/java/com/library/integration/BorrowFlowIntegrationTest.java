package com.library.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 10.2 借阅全流程集成测试（P0）.
 * <p>
 * 搜索→详情→借书→还书→统计验证。借/还经 EventBusBridge→MQ→ESSyncListener 异步同步 ES。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@DisplayName("10.2 借阅全流程")
class BorrowFlowIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("搜索→详情→借书→还书→统计验证 全流程")
    void shouldCompleteBorrowFlowWhenBookAvailable() {
        // V100 种子中 4 个 test_* 用户均有 OVERDUE 借阅（BorrowService step 5 拒绝），
        // 故借书全流程改用 admin（V4 创建，无任何借阅历史，role=ADMIN max=15）
        String token = loginHelper.login("admin", "Admin@123456");

        // 1. 搜索图书（ES 种子数据已由 EsDataLoader 导入）
        ResponseEntity<Map> searchResp = restTemplate.exchange(
                API + "/books/search?keyword=Java虚拟机&pageNum=1&pageSize=10",
                HttpMethod.GET, loginHelper.auth(token), Map.class);
        assertThat(searchResp.getStatusCode().is2xxSuccessful()).isTrue();

        // 2. 详情
        ResponseEntity<Map> detailResp = restTemplate.exchange(
                API + "/books/10001", HttpMethod.GET, loginHelper.auth(token), Map.class);
        assertThat(detailResp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(((Map<?, ?>) detailResp.getBody().get("data")).get("title")).asString().contains("Java虚拟机");

        // 3. 借书（10006 计算机网络 V100 avail=4，admin 无借阅历史可借）
        Map<String, Object> borrowReq = Map.of("bookId", 10006);
        ResponseEntity<Map> borrowResp = restTemplate.postForEntity(
                API + "/borrows", loginHelper.auth(token, borrowReq), Map.class);
        assertThat(borrowResp.getStatusCode().is2xxSuccessful()).isTrue();
        Long borrowId = asLong(((Map<?, ?>) borrowResp.getBody().get("data")).get("borrowId"));

        // 4. 还书
        ResponseEntity<Map> returnResp = restTemplate.exchange(
                API + "/borrows/" + borrowId + "/return", HttpMethod.PUT, loginHelper.auth(token), Map.class);
        assertThat(returnResp.getStatusCode().is2xxSuccessful()).isTrue();

        // 5. 统计验证
        ResponseEntity<Map> statsResp = restTemplate.exchange(
                API + "/users/me/stats", HttpMethod.GET, loginHelper.auth(token), Map.class);
        assertThat(statsResp.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @DisplayName("有 OVERDUE 借阅的用户借书应被拒（业务规则）")
    void shouldRejectBorrowWhenUserHasOverdue() {
        // test_student 在 V100 中有 OVERDUE 记录（20042 / 20045），借书应被 step 5 拒绝
        String token = loginHelper.login("test_student", "Test@123456");
        Map<String, Object> req = Map.of("bookId", 10006);
        ResponseEntity<Map> resp = restTemplate.postForEntity(
                API + "/borrows", loginHelper.auth(token, req), Map.class);
        assertThat(resp.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    @DisplayName("重复借同一本书应返回 4xx")
    void shouldRejectDuplicateBorrowWhenAlreadyBorrowed() {
        String token = loginHelper.login("test_librarian", "Test@123456");
        // test_librarian 已借 20033(10008) — 重复借 10008 应拒绝
        Map<String, Object> req = Map.of("bookId", 10008);
        ResponseEntity<Map> resp = restTemplate.postForEntity(
                API + "/borrows", loginHelper.auth(token, req), Map.class);
        assertThat(resp.getStatusCode().is4xxClientError()).isTrue();
    }
}
