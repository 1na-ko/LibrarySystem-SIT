package com.library.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 10.7 权限矩阵验证集成测试（P0）.
 * <p>
 * 按架构文档 §2.3 验证 RBAC 边界。覆盖关键角色×端点组合（Student/Teacher/Librarian/Acquisitor/Admin
 * 各调代表性 API），断言 200/403。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@DisplayName("10.7 权限矩阵验证")
class RbacMatrixIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("Student 访问公共图书检索应 200")
    void shouldReturn200WhenStudentCallsBookSearch() {
        String token = loginHelper.login("test_student", "Test@123456");
        ResponseEntity<Map> resp = restTemplate.exchange(
                API + "/books/search?keyword=Java&pageNum=1&pageSize=10",
                HttpMethod.GET, loginHelper.auth(token), Map.class);
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @DisplayName("Student 调用管理端用户列表应 403")
    void shouldReturn403WhenStudentCallsAdminUsers() {
        String token = loginHelper.login("test_student", "Test@123456");
        ResponseEntity<Map> resp = restTemplate.exchange(
                API + "/admin/users?page=1&size=20", HttpMethod.GET, loginHelper.auth(token), Map.class);
        assertThat(resp.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    @DisplayName("Librarian 访问用户管理应 200")
    void shouldReturn200WhenLibrarianCallsAdminUsers() {
        String token = loginHelper.login("test_librarian", "Test@123456");
        ResponseEntity<Map> resp = restTemplate.exchange(
                API + "/admin/users?page=1&size=20", HttpMethod.GET, loginHelper.auth(token), Map.class);
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @DisplayName("Acquisitor 访问采编预测应 200")
    void shouldReturn200WhenAcquisitorCallsPredict() {
        String token = loginHelper.login("test_acquisitor", "Test@123456");
        ResponseEntity<Map> resp = restTemplate.exchange(
                API + "/acquisition/predict?subjectId=1&months=3", HttpMethod.GET, loginHelper.auth(token), Map.class);
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @DisplayName("Acquisitor 调用借阅 API 应 403（无借阅角色）")
    void shouldReturn403WhenAcquisitorCallsBorrowApi() {
        String token = loginHelper.login("test_acquisitor", "Test@123456");
        Map<String, Object> req = Map.of("bookId", 10006);
        ResponseEntity<Map> resp = restTemplate.postForEntity(
                API + "/borrows", loginHelper.auth(token, req), Map.class);
        assertThat(resp.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    @DisplayName("Admin 访问流通统计 Dashboard 应 200")
    void shouldReturn200WhenAdminCallsDashboard() {
        String token = loginHelper.login("admin", "Admin@123456");
        ResponseEntity<Map> resp = restTemplate.exchange(
                API + "/admin/stats/dashboard", HttpMethod.GET, loginHelper.auth(token), Map.class);
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
    }
}
