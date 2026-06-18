package com.library.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * 10.10 搜索 QPS 性能测试.
 * <p>
 * 用 CountDownLatch + 线程池并发搜索，计时断言 QPS。
 * 集成测试环境（Testcontainers ES）阈值放宽至 ≥30 QPS；
 * 生产单节点目标 ≥500（缓存命中）/ ≥100（ES 穿透），见架构文档 §11。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@DisplayName("10.10 搜索 QPS 性能")
class SearchPerformanceTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("热点词并发搜索应达到合理QPS")
    void shouldReachReasonableQpsWhenConcurrentSearch() throws Exception {
        String token = loginHelper.login("test_student", "Test@123456");
        String url = API + "/books/search?keyword=Java&pageNum=1&pageSize=10";
        HttpEntity<Void> auth = loginHelper.auth(token);

        // 预热缓存
        restTemplate.exchange(url, HttpMethod.GET, auth, Map.class);

        int threads = 20;
        int requestsPerThread = 25;
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(threads);
        AtomicInteger success = new AtomicInteger(0);

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    startGate.await();
                    for (int j = 0; j < requestsPerThread; j++) {
                        ResponseEntity<Map> r = restTemplate.exchange(url, HttpMethod.GET, auth, Map.class);
                        if (r.getStatusCode().is2xxSuccessful()) success.incrementAndGet();
                    }
                } catch (Exception ignored) {
                } finally {
                    endGate.countDown();
                }
            });
        }

        long start = System.nanoTime();
        startGate.countDown();
        endGate.await(120, SECONDS);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        pool.shutdownNow();

        double qps = success.get() * 1000.0 / Math.max(elapsedMs, 1);
        assertThat(qps).isGreaterThan(30.0);
    }
}
