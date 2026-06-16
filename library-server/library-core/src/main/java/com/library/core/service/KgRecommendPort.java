package com.library.core.service;

import java.util.Map;

/**
 * KG 推荐查询端口（SPI）.
 * <p>
 * 由 {@code library-knowledge-graph} 模块提供实现（条件注入），
 * {@link KGBasedRecommendService} 适配器通过 {@code ObjectProvider} 延迟注入，
 * 存在时转发至真实 KG 多跳推荐，不存在时返回空 Map（桩行为）。
 * <p>
 * 依赖方向：core ← knowledge-graph（kg 依赖 core），core 不能反向依赖 kg。
 * 此 Port 接口破除此循环依赖约束。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public interface KgRecommendPort {

    /**
     * 基于知识图谱获取与用户借阅历史相关的图书.
     *
     * @param userId 目标用户 ID
     * @param topN   返回候选数
     * @return 候选图书 ID → KG 分数（0-1），KG 未就绪时返回空 Map
     */
    Map<Long, Double> recommend(Long userId, int topN);
}
