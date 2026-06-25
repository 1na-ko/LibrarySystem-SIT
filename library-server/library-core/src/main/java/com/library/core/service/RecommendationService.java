package com.library.core.service;

import com.library.core.vo.BookRecommendVO;

import java.util.List;

/**
 * 个性化推荐引擎服务接口.
 * <p>
 * 编排多路召回（CF + Content + KG）、加权融合、精排、
 * LLM 个性化理由生成（含模板降级）的全流程。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public interface RecommendationService {

    /**
     * 为指定用户生成个性化图书推荐.
     *
     * @param userId 用户 ID
     * @param limit  返回条数上限（1-50）
     * @return 推荐结果列表（含图书、分数、理由），无结果时返回空列表
     */
    List<BookRecommendVO> recommend(Long userId, int limit);

    /**
     * 快速推荐（仅召回+融合+精排+模板理由，不调用 LLM）.
     * <p>
     * 用于流式端点先于 LLM 返回书目列表，让前端立即展示推荐图书；
     * LLM 生成的个性化导语由调用方通过 {@code LlmService.chatStream} 单独流式推送.
     *
     * @param userId 用户 ID
     * @param limit  返回条数上限（1-50）
     * @return 推荐结果列表（含图书、分数、模板理由），无结果时返回空列表
     */
    List<BookRecommendVO> recommendBooksQuick(Long userId, int limit);

    /**
     * 构建推荐导语 Prompt（基于用户借阅历史 + 推荐书目），供流式 LLM 生成.
     * <p>
     * 导语是一段自然语言（50-100 字），解释为什么推荐这些书，语气亲切.
     *
     * @param userId         用户 ID
     * @param recommendations 推荐书目（由 {@link #recommendBooksQuick} 产生）
     * @return LLM Prompt；若用户无借阅历史返回 null（调用方应跳过流式）
     */
    String buildReasonPrompt(Long userId, List<BookRecommendVO> recommendations);
}
