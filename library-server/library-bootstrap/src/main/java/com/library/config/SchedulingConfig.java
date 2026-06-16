package com.library.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定时任务调度配置.
 * <p>
 * 启用 Spring 的 {@code @Scheduled} 注解支持。
 * 当前计划任务：
 * <ul>
 *   <li>{@code OverdueCheckJob} — 每天凌晨 3:00 超期检查</li>
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
}
