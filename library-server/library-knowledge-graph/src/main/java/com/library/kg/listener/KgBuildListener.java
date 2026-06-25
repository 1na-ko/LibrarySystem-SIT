package com.library.kg.listener;

import com.library.core.event.EventBusConstants;
import com.library.kg.repository.Neo4jRepository;
import com.library.kg.service.GraphBuildService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 知识图谱自动构建消费者.
 * <p>
 * 阶段 10 改为 {@code @RabbitListener} 消费 {@code q.kg-build} 队列（原
 * {@code @TransactionalEventListener(AFTER_COMMIT)} 的提交后语义由 {@code EventBusBridge} 保留）。
 * <p>
 * 消费图书增/改/删事件，异步同步 MySQL → Neo4j。Neo4j 不可用时仅记日志，不阻塞主流程。
 * 构建失败由 Spring AMQP RetryTemplate（3 次指数退避）重试，耗尽进死信队列。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class KgBuildListener {

    private final GraphBuildService graphBuildService;
    private final Neo4jRepository neo4jRepository;

    public KgBuildListener(GraphBuildService graphBuildService, Neo4jRepository neo4jRepository) {
        this.graphBuildService = graphBuildService;
        this.neo4jRepository = neo4jRepository;
    }

    /**
     * 消费 MQ 图书事件 → 同步 Neo4j.
     *
     * @param bookId     图书 ID（消息体）
     * @param routingKey 事件类型（book.created/updated/deleted）
     */
    @RabbitListener(queues = EventBusConstants.QUEUE_KG_BUILD)
    public void onBookEvent(Long bookId,
                            @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) {
        if (EventBusConstants.RK_BOOK_DELETED.equals(routingKey)) {
            deleteFromNeo4j(bookId);
            return;
        }
        graphBuildService.buildGraph(bookId);
        log.info("KG 构建：bookId={}, routingKey={}", bookId, routingKey);
    }

    /**
     * 删除图书 → 清理 Neo4j 节点及关联关系.
     * <p>
     * 容错：节点不存在不视为异常（可能从未构建过图谱）。
     */
    private void deleteFromNeo4j(Long bookId) {
        log.info("KG 同步删除: bookId={}", bookId);
        try {
            String cypher = "MATCH (b:Book {id: $bookId}) DETACH DELETE b";
            neo4jRepository.execute(cypher, Map.of("bookId", bookId));
            log.info("KG 删除同步成功: bookId={}", bookId);
        } catch (Exception e) {
            log.warn("KG 删除同步失败（可能节点不存在）: bookId={}, error={}", bookId, e.getMessage());
        }
    }
}
