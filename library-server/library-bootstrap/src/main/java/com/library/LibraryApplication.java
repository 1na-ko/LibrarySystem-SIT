package com.library;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 图书馆智能管理系统 — Spring Boot 启动入口.
 *
 * <p>组件扫描范围：{@code com.library}（含本启动模块自身及所有子模块）：
 * <ul>
 *   <li>{@code com.library} — 本模块（library-bootstrap）：启动类与全局配置</li>
 *   <li>{@code com.library.common} — 公共基础设施</li>
 *   <li>{@code com.library.ai} — AI 基础设施</li>
 *   <li>{@code com.library.core} — 核心业务</li>
 *   <li>{@code com.library.kg} — 知识图谱</li>
 *   <li>{@code com.library.acquisition} — 智能采编</li>
 *   <li>{@code com.library.security} — 安全认证</li>
 * </ul>
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@SpringBootApplication(scanBasePackages = "com.library")
public class LibraryApplication {

    public static void main(String[] args) {
        SpringApplication.run(LibraryApplication.class, args);
    }
}
