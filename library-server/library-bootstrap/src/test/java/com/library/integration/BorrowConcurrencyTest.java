package com.library.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * 10.11 借阅并发安全性能测试.
 * <p>
 * 并发抢借 1 本库存 → 仅 1 人成功（验证 BorrowServiceImpl 的 Redisson 分布式锁 + DB 乐观锁防超卖）。
 * 用 20 并发（V100 用户数限制，验证并发安全足够；计划目标 50，生产环境可扩展）。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@DisplayName("10.11 借阅并发安全")
class BorrowConcurrencyTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("并发抢借1本库存应仅1人成功（防超卖）")
    void shouldAllowOnlyOneSuccessWhenConcurrentBorrowOneCopy() throws Exception {
        // 创建 avail=1 的书
        String librarianToken = loginHelper.login("test_librarian", "Test@123456");
        Map<String, Object> req = Map.of(
                "isbn", "978-7-111-66666-6", "title", "并发抢借测试书",
                "author", "测试", "categoryId", 101, "totalCopies", 1);
        ResponseEntity<Map> createResp = restTemplate.postForEntity(
                API + "/admin/books", loginHelper.auth(librarianToken, req), Map.class);
        Long bookId = asLong(((Map<?, ?>) createResp.getBody().get("data")).get("id"));

        int n = 20;
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(n);
        AtomicInteger success = new AtomicInteger(0);
        ExecutorService pool = Executors.newFixedThreadPool(n);

        for (int i = 0; i < n; i++) {
            final int idx = i;
            pool.submit(() -> {
                try {
                    // 每线程注册独立临时用户（避免单用户重复借拒绝）
                    String username = "conc" + idx;
                    Map<String, String> reg = Map.of(
                            "username", username, "password", "Test@123456",
                            "realName", "并发" + idx, "email", "conc" + idx + "@t.edu.cn",
                            "phone", String.format("139%08d", idx));
                    restTemplate.postForEntity(API + "/auth/register", reg, Map.class);
                    startGate.await();
                    String token = loginHelper.login(username, "Test@123456");
                    ResponseEntity<Map> r = restTemplate.postForEntity(
                            API + "/borrows", loginHelper.auth(token, Map.of("bookId", bookId)), Map.class);
                    if (r.getStatusCode().is2xxSuccessful()) success.incrementAndGet();
                } catch (Exception ignored) {
                } finally {
                    endGate.countDown();
                }
            });
        }

        startGate.countDown();
        endGate.await(120, SECONDS);
        pool.shutdownNow();

        // 仅 1 人成功，其余库存不足 4xx（防超卖）
        assertThat(success.get()).isEqualTo(1);
    }
}
