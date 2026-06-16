package com.library.acquisition.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * 采编模块 MyBatis Mapper 扫描配置.
 * <p>
 * 独立限定扫描路径，避免与 library-core 的 Mapper 扫描冲突。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Configuration
@MapperScan("com.library.acquisition.mapper")
public class AcquisitionMapperConfig {
}
