package com.library.security.controller;

import com.library.common.result.Result;
import com.library.core.service.RecommendationService;
import com.library.core.vo.BookRecommendVO;
import com.library.security.aspect.RequireRole;
import com.library.security.context.SecurityUtils;
import com.library.core.enums.RoleEnum;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 个性化图书推荐控制器.
 * <p>
 * 提供基于协同过滤、内容分析和知识图谱的混合推荐端点。
 * 用户 ID 从 SecurityContext 获取，确保仅返回当前用户的推荐。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/users/me")
@RequiredArgsConstructor
@Validated
public class RecommendationController {

    private final RecommendationService recommendationService;

    /**
     * 获取个性化图书推荐.
     * <p>
     * 返回 Top-N 推荐图书列表，每项含分数（0-1）和个性化推荐理由。
     * 无借阅历史的新用户返回空列表。
     *
     * @param limit 返回条数上限（默认 20，最小 1，最大 50）
     * @return 推荐图书列表（含推荐分数与理由）
     */
    @GetMapping("/recommendations")
    @RequireRole({RoleEnum.STUDENT, RoleEnum.TEACHER, RoleEnum.LIBRARIAN, RoleEnum.ADMIN})
    public Result<List<BookRecommendVO>> getRecommendations(
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit) {
        long userId = SecurityUtils.getCurrentUserId();
        List<BookRecommendVO> recommendations = recommendationService.recommend(userId, limit);
        return Result.success(recommendations);
    }
}
