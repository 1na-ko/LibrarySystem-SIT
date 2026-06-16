package com.library.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务线程池配置.
 * <p>
 * 用于领域事件的异步处理（ES 同步、预约通知等）以及推荐引擎的并行召回。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@EnableAsync
@Configuration
public class AsyncConfig {

    /** 核心线程数 */
    private static final int CORE_POOL_SIZE = 8;
    /** 最大线程数 */
    private static final int MAX_POOL_SIZE = 16;
    /** 队列容量（KG 构建/ES 同步事件洪峰下，100 易触发 CallerRunsPolicy 拖住调用线程） */
    private static final int QUEUE_CAPACITY = 500;
    /** 线程名前缀 */
    private static final String THREAD_NAME_PREFIX = "library-async-";

    @Bean("taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CORE_POOL_SIZE);
        executor.setMaxPoolSize(MAX_POOL_SIZE);
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setThreadNamePrefix(THREAD_NAME_PREFIX);
        // 调用者线程执行拒绝策略：队列满时由调用线程执行，保证不丢任务
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 线程池关闭时等待任务完成
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();

        log.info("异步线程池已初始化: core={}, max={}, queue={}",
                CORE_POOL_SIZE, MAX_POOL_SIZE, QUEUE_CAPACITY);
        return executor;
    }
}
