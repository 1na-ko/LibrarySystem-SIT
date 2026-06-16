package com.library.core.service.impl;

import com.library.core.service.KGBasedRecommendService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

/**
 * 知识图谱推荐桩实现.
 * <p>
 * 阶段 6（KG 模块未就绪）直接返回空列表。
 * 阶段 7 完成后替换为 {@code @ConditionalOnBean(Neo4jClient.class)} 的真实实现。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class KGBasedRecommendServiceImpl implements KGBasedRecommendService {

    @Override
    public Map<Long, Double> recommend(Long userId, int topN) {
        log.debug("KG 推荐未就绪（阶段 7 实施），返回空列表");
        return Collections.emptyMap();
    }
}
