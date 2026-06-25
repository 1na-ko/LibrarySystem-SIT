package com.library.kg.repository;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.stereotype.Component;

/**
 * GDS 插件可用性探测器.
 * <p>
 * 启动期通过 {@code SHOW PROCEDURES} 探测 Neo4j GDS 插件是否可用，
 * 结果缓存为单例 Bean。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class GdsAvailabilityProvider {

    private final Driver driver;

    /** GDS 是否可用（探测后缓存） */
    @Getter
    private volatile boolean available = false;

    public GdsAvailabilityProvider(Driver driver) {
        this.driver = driver;
    }

    @PostConstruct
    public void probe() {
        try (Session session = driver.session()) {
            var result = session.run(
                    "SHOW PROCEDURES YIELD name WHERE name STARTS WITH 'gds.' RETURN count(*) AS c");
            if (result.hasNext()) {
                long cnt = result.next().get("c").asLong();
                this.available = cnt > 0;
                log.info("GDS 可用性探测结果: available={}, procedureCount={}", available, cnt);
            } else {
                this.available = false;
                log.info("GDS 可用性探测结果: available=false");
            }
        } catch (Exception e) {
            log.warn("GDS 探测失败，降级为纯 Cypher / Java 算法: {}", e.getMessage());
            this.available = false;
        }
    }
}
