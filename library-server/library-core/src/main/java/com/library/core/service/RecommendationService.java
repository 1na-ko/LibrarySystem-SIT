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
}
