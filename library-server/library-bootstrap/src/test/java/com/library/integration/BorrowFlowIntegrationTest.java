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
        String token = loginHelper.login("test_student", "Test@123456");

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

        // 3. 借书（10006 计算机网络 avail=4，test_student 已借4本达上限5，借第5本）
        Map<String, Object> borrowReq = Map.of("bookId", 10006);
        ResponseEntity<Map> borrowResp = restTemplate.postForEntity(
                API + "/borrows", loginHelper.auth(token, borrowReq), Map.class);
        assertThat(borrowResp.getStatusCode().is2xxSuccessful()).isTrue();
        Long borrowId = ((Number) ((Map<?, ?>) borrowResp.getBody().get("data")).get("borrowId")).longValue();

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
