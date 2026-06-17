package com.library.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 定时任务调度配置.
 * <p>
 * 启用 Spring 的 {@code @Scheduled} 注解支持，配置独立线程池。
 * 当前计划任务：
 * <ul>
 *   <li>{@code OverdueCheckJob} — 每天凌晨 3:00 超期检查</li>
 *   <li>{@code ReservationExpireJob} — 每小时 过期处理</li>
 *   <li>{@code ReservationZsetReconcileJob} — 每天凌晨 4:00 ZSET 对账</li>
 *   <li>{@code EsRebuildJob} — 每周日 4:00 ES 全量重建</li>
 * </ul>
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@EnableScheduling
@Configuration
public class SchedulingConfig {

    public SchedulingConfig() {
        log.info("定时任务调度已启用");
    }

    /**
     * 定时任务专用线程池.
     * <p>
     * poolSize=4（4 个任务可并行），队列 CallerRunsPolicy 防丢任务，
     * errorHandler 仅 log 不抛异常，避免单任务失败导致调度器终止。
     */
    @Bean("schedulingTaskExecutor")
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("library-schedule-");
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        scheduler.setErrorHandler(t ->
                log.error("定时任务异常: {}", t.getMessage(), t));
        return scheduler;
    }
}
