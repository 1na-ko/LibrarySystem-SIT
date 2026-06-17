package com.library.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 10.16 越权访问防护安全测试.
 * <p>
 * 与 10.7 权限矩阵的区别：10.7 是全矩阵边界验证，本类聚焦典型越权场景的 403 明确断言。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@DisplayName("10.16 越权访问防护")
class CrossRoleSecurityTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("Student 调用管理端用户列表应 403")
    void shouldReturn403WhenStudentCallsAdminUsersApi() {
        String token = loginHelper.login("test_student", "Test@123456");
        ResponseEntity<Map> resp = restTemplate.exchange(
                API + "/admin/users?page=1&size=20", HttpMethod.GET, loginHelper.auth(token), Map.class);
        assertThat(resp.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    @DisplayName("Student 调用管理端图书编目应 403")
    void shouldReturn403WhenStudentCallsAdminBooksApi() {
        String token = loginHelper.login("test_student", "Test@123456");
        Map<String, Object> req = Map.of(
                "isbn", "978-7-111-55555-5", "title", "越权测试", "author", "x",
                "categoryId", 101, "totalCopies", 1);
        ResponseEntity<Map> resp = restTemplate.postForEntity(
                API + "/admin/books", loginHelper.auth(token, req), Map.class);
        assertThat(resp.getStatusCode().value()).isEqualTo(403);
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
    @DisplayName("Librarian 调用采编预测应 403（无采编权限）")
    void shouldReturn403WhenLibrarianCallsAcquisitionApi() {
        String token = loginHelper.login("test_librarian", "Test@123456");
        ResponseEntity<Map> resp = restTemplate.exchange(
                API + "/acquisition/predict?subjectId=1&months=3", HttpMethod.GET, loginHelper.auth(token), Map.class);
        assertThat(resp.getStatusCode().value()).isEqualTo(403);
    }
}
