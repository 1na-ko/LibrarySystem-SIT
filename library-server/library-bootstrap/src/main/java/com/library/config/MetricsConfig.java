package com.library.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.TaskScheduler;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Prometheus / Micrometer 指标配置.
 * <p>
 * 暴露自定义业务指标：
 * <ul>
 *   <li>{@code library_borrows_total} — 借阅/归还计数（在 Service 中递增）</li>
 *   <li>{@code library_searches_total} — 搜索计数（在 Service 中递增）</li>
 *   <li>{@code library_reservations_queue_size} — 预约队列总大小（Gauge 采样）</li>
 * </ul>
 * <p>
 * Counter 递增在各自 Service 实现中通过注入 {@code MeterRegistry} 完成。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class MetricsConfig {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 通用标签：标识应用名称.
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config().commonTags("application", "library-system");
    }

    /**
     * 预约队列大小 Gauge（独立 daemon 线程每 60 秒 Redis SCAN 采样）.
     * <p>
     * 注意：仅通过 {@link MeterBinder} 注册一次——若拆为两个 Bean 会因 Micrometer
     * {@code putIfAbsent} 语义导致后注册的 Gauge 被静默丢弃，指标永久为 0。
     */
    /**
     * 预约队列大小 Gauge（复用 Spring 管理的 TaskScheduler 每 60 秒 Redis SCAN 采样）.
     * <p>
     * 采样任务委托 {@code schedulingTaskExecutor}（见 {@link SchedulingConfig}）调度，
     * 替代原裸 daemon Thread——线程由 Spring 容器统一管理生命周期，与项目调度规范一致。
     * <p>
     * 注意：仅通过 {@link MeterBinder} 注册一次——若拆为两个 Bean 会因 Micrometer
     * {@code putIfAbsent} 语义导致后注册的 Gauge 被静默丢弃，指标永久为 0。
     */
    @Bean
    public MeterBinder reservationQueueSizeBinder(
            @Qualifier("schedulingTaskExecutor") TaskScheduler taskScheduler) {
        return registry -> {
            AtomicLong gauge = new AtomicLong(0);
            Gauge.builder("library_reservations_queue_size", gauge::get)
                    .description("Total entries across all reservation ZSET queues")
                    .register(registry);
            // 每 60 秒采样一次（首次立即执行，之后固定速率）
            taskScheduler.scheduleAtFixedRate(() -> sampleQueueSize(gauge), Duration.ofSeconds(60));
        };
    }

    /**
     * 采样所有预约队列 ZSET 的总大小并更新 Gauge.
     */
    private void sampleQueueSize(AtomicLong gauge) {
        try {
            long total = 0;
            try (Cursor<String> cursor = (Cursor<String>) redisTemplate.scan(
                    ScanOptions.scanOptions()
                            .match("reservation:queue:*")
                            .count(100).build())) {
                while (cursor.hasNext()) {
                    String key = cursor.next();
                    Long size = redisTemplate.opsForZSet().size(key);
                    if (size != null) {
                        total += size;
                    }
                }
            }
            gauge.set(total);
        } catch (Exception e) {
            log.debug("预约队列 Gauge 采样失败: {}", e.getMessage());
            gauge.set(-1);
        }
    }
}
