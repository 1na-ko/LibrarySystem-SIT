package com.library.integration;

import com.library.core.config.EsIndexInitializer;
import com.library.core.event.EventBusConstants;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

/**
 * 集成测试基类.
 * <p>
 * 5 中间件（MySQL/Redis/ES+IK/Neo4j/RabbitMQ）由 docker-compose 预启动，端口/密码与
 * {@code application.yml} 默认值一致，无需 {@code @ServiceConnection} 注入连接。每测试类
 * {@code @BeforeAll} 将 5 中间件重置到 V100 种子状态，实现类间数据隔离——替代原 Testcontainers
 * 的"每 JVM 新容器"隐式隔离（Testcontainers 1.21.3 与 Docker Desktop 29 不兼容已移除）。
 * <p>
 * 重置粒度为每测试类（{@link TestInstance.Lifecycle#PER_CLASS}），共享 Spring 上下文不加
 * {@code @DirtiesContext}：17 类 × ~6s 重置开销可接受；现有断言按"类内方法共享种子状态"设计，
 * 方法级隔离会破坏断言。每类开始时中间件状态确定相同，类间执行顺序无关。
 * <p>
 * 前置：{@code docker-compose up -d} 启动 5 中间件（ES 首次需 {@code make es-ik-check} 装 IK + restart）。
 * 运行：{@code make itest}（等价 {@code mvn test -Pintegration}）。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractIntegrationTest {

    /**
     * 全局 API 路径前缀（保留为空字符串）.
     * <p>
     * Spring Boot {@code TestRestTemplate} 在 {@code @SpringBootTest(RANDOM_PORT)} 下，
     * 会通过 {@code LocalServerPort} 自动构造包含 {@code server.servlet.context-path}
     * 的 baseUrl（{@code http://localhost:{port}/api/v1}），测试代码直接传相对路径
     * （如 {@code "/auth/register"}）即可，再叠加 {@code /api/v1} 前缀会变成
     * {@code /api/v1/api/v1/auth/register} 而 401。
     * <p>
     * 保留常量名以兼容现有 16+ 测试类的 {@code API + "/xxx"} 写法，不强制改 16 个测试文件。
     */
    protected static final String API = "";

    /**
     * 兼容性 ID 解析：项目 {@code JacksonConfig} 全局把 Long 序列化为 String（防 JS 大数精度丢失），
     * 测试拿到 JSON {@code data.id} 是 String，按 {@code (Number) idValue} cast 会抛
     * ClassCastException。本方法统一处理 String/Number 两种类型，返回 {@link Long}。
     *
     * @param value JSON 反序列化得到的 ID 字段（可能是 String 或 Number）
     * @return 对应 Long 值；value 为 null 返回 null
     */
    protected static Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        return Long.valueOf(value.toString());
    }

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected EsDataLoader esDataLoader;

    @Autowired
    protected LoginHelper loginHelper;

    /** 中间件重置组件 */
    @Autowired
    private Flyway flyway;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private EsIndexInitializer esIndexInitializer;

    @Autowired
    private Driver neo4jDriver;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /** 需清理的 MQ 队列（业务队列 + 死信队列） */
    private static final List<String> MQ_QUEUES = List.of(
            EventBusConstants.QUEUE_ES_SYNC,
            EventBusConstants.QUEUE_RESERVATION_NOTIFY,
            EventBusConstants.QUEUE_KG_BUILD,
            EventBusConstants.DLQ);

    /**
     * 每测试类开始前重置 5 中间件到 V100 种子状态，保证类间数据隔离.
     * <p>
     * 顺序：先清 MQ 残留消息（防重置期间被消费）→ MySQL Flyway clean+migrate → Redis flushDb
     * → ES 重建索引+种子 → Neo4j 清空。重置过程不发布领域事件，无 MQ 干扰。
     */
    @BeforeAll
    void resetMiddlewares() {
        resetRabbitMq();
        resetMySQL();
        resetRedis();
        resetElasticsearch();
        resetNeo4j();
        log.info("集成测试中间件已重置到 V100 种子状态");
    }

    /**
     * MySQL：Flyway clean + migrate，清空所有业务数据并重跑 V1-V7 建表 + V100 种子.
     * <p>
     * 解决用户名冲突（authflow_user、并发测试临时用户）、借阅/预约/谈判/测试书残留。
     * 前置：application-test.yml 的 {@code spring.flyway.clean-disabled: false}。
     */
    private void resetMySQL() {
        flyway.clean();
        flyway.migrate();
        log.debug("MySQL 已重置（Flyway clean + migrate）");
    }

    /**
     * Redis：flushDb 清空所有限流令牌桶/Token 黑名单/预约 ZSET/搜索缓存残留.
     */
    private void resetRedis() {
        redisTemplate.getConnectionFactory().getConnection().flushDb();
        log.debug("Redis 已重置（flushDb）");
    }

    /**
     * ES：删除 books 索引并重建（IK 分析器 + Completion Suggester），再同步 V100 种子图书 10001-10020.
     * <p>
     * 不用 deleteAll——索引 mapping 可能因测试写入异常文档漂移，recreateIndex 才彻底。
     */
    private void resetElasticsearch() {
        try {
            esIndexInitializer.recreateIndex();
            esDataLoader.bulkSyncSeedBooks();
            log.debug("ES 已重置（recreateIndex + bulkSyncSeedBooks）");
        } catch (Exception e) {
            throw new IllegalStateException("ES 重置失败，请确认 ES 已启动且 IK 分词器已安装（make es-ik-check）", e);
        }
    }

    /**
     * Neo4j：清空所有节点与关系.
     * <p>
     * 不 rebuild——{@code KgTracingPerformanceTest} 自带 {@code /admin/kg/rebuild-all}，
     * {@code RecommendationKgFlowIntegrationTest} 图谱查询断言容错空图谱。避免 16 个不依赖图谱的类
     * 都承担 rebuild-all（HanLP 降级 NER/RE）~15s 开销。
     */
    private void resetNeo4j() {
        try (Session session = neo4jDriver.session()) {
            session.run("MATCH (n) DETACH DELETE n").consume();
        }
        log.debug("Neo4j 已重置（DETACH DELETE）");
    }

    /**
     * RabbitMQ：purgeQueue 清空 4 队列残留消息.
     * <p>
     * 防上一测试类未消费完的 {@code book.*} 事件在下一类启动时被 ESSyncListener/ReservationNotifier
     * 误消费，导致 ES 意外同步或预约误通知。
     */
    private void resetRabbitMq() {
        RabbitAdmin admin = new RabbitAdmin(rabbitTemplate.getConnectionFactory());
        for (String queue : MQ_QUEUES) {
            try {
                admin.purgeQueue(queue, false);
            } catch (Exception e) {
                log.debug("队列 {} 清空跳过（可能尚未声明）: {}", queue, e.getMessage());
            }
        }
        log.debug("RabbitMQ 已重置（purgeQueue × {}）", MQ_QUEUES.size());
    }
}
