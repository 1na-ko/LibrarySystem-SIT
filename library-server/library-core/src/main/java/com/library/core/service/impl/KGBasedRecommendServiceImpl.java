package com.library.core.service.impl;

import com.library.core.service.KGBasedRecommendService;
import com.library.core.service.KgRecommendPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

/**
 * 知识图谱推荐适配器.
 * <p>
 * 阶段 6 桩返回空 Map；阶段 7 完成后通过 {@link ObjectProvider} 自动注入
 * {@link KgRecommendPort}（由 {@code library-knowledge-graph} 模块提供，
 * {@code @ConditionalOnBean(Neo4jClient.class)}），存在时转发至 KG 多跳推荐，
 * 异常时降级返回空 Map。调用方 {@code RecommendationServiceImpl} 无需改动。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class KGBasedRecommendServiceImpl implements KGBasedRecommendService {

    private final ObjectProvider<KgRecommendPort> portProvider;

    public KGBasedRecommendServiceImpl(ObjectProvider<KgRecommendPort> portProvider) {
        this.portProvider = portProvider;
    }

    @Override
    public Map<Long, Double> recommend(Long userId, int topN) {
        KgRecommendPort port = portProvider.getIfAvailable();
        if (port == null) {
            log.debug("KG 推荐未就绪（KgRecommendPort Bean 不存在），返回空列表");
            return Collections.emptyMap();
        }
        try {
            return port.recommend(userId, topN);
        } catch (Exception e) {
            log.warn("KG 推荐失败，降级空集: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }
}
