package com.library.security.controller;

import com.library.ai.llm.LlmService;
import com.library.common.result.Result;
import com.library.core.service.RecommendationService;
import com.library.core.vo.BookRecommendVO;
import com.library.security.aspect.RequireRole;
import com.library.security.context.SecurityUtils;
import com.library.core.enums.RoleEnum;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

/**
 * 个性化图书推荐控制器.
 * <p>
 * 提供基于协同过滤、内容分析和知识图谱的混合推荐端点。
 * 用户 ID 从 SecurityContext 获取，确保仅返回当前用户的推荐。
 * <p>
 * 除同步端点外，提供 SSE 流式端点 {@code /recommendations/stream}：
 * 先秒推推荐书目（前端立即展示），再逐 token 流式推送 LLM 生成的推荐导语（打字机效果）。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/users/me")
@RequiredArgsConstructor
@Validated
public class RecommendationController {

    private final RecommendationService recommendationService;

    /** LLM 服务可能因 API Key 缺失而不存在（流式导语降级为静态文案） */
    @Autowired(required = false)
    private LlmService llmService;

    /**
     * 获取个性化图书推荐（同步，含 LLM 每书理由）.
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

    /**
     * SSE 流式推荐：书目秒回 + LLM 导语逐 token 流式.
     * <p>
     * 事件序列：
     * <ol>
     *   <li>{@code books}：推荐书目列表 JSON（召回+融合+模板理由，&lt;1s 返回）</li>
     *   <li>{@code reason}（多次）：LLM 生成的导语增量 token，前端逐字拼接</li>
     *   <li>{@code done}：流式结束</li>
     * </ol>
     * 无借阅历史时仅推 books(空) + done。
     *
     * @param limit 返回条数上限（默认 20，最小 1，最大 50）
     * @return SseEmitter
     */
    @GetMapping(value = "/recommendations/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RequireRole({RoleEnum.STUDENT, RoleEnum.TEACHER, RoleEnum.LIBRARIAN, RoleEnum.ADMIN})
    public SseEmitter streamRecommendations(
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit) {
        long userId = SecurityUtils.getCurrentUserId();
        // 60s 超时：LLM 流式导语通常 10-30s，留足余量
        SseEmitter emitter = new SseEmitter(60_000L);
        emitter.onTimeout(() -> {
            log.warn("推荐 SSE 流超时: userId={}", userId);
            emitter.complete();
        });
        emitter.onError(e -> log.warn("推荐 SSE 流异常: userId={}, err={}", userId, e.getMessage()));

        try {
            // 1. 快速书目（秒回，不调 LLM）
            List<BookRecommendVO> books = recommendationService.recommendBooksQuick(userId, limit);
            emitter.send(SseEmitter.event().name("books").data(books, MediaType.APPLICATION_JSON));

            // 2. 构建导语 Prompt
            String prompt = recommendationService.buildReasonPrompt(userId, books);
            if (prompt == null || llmService == null) {
                // 无借阅历史或无 LLM：推静态文案后结束
                String fallback = books.isEmpty()
                        ? "暂无足够借阅记录生成推荐，多借几本书再来看看吧。"
                        : "以上书目基于您的借阅历史精选，希望您喜欢。";
                emitter.send(SseEmitter.event().name("reason").data(fallback));
                emitter.send(SseEmitter.event().name("done").data(""));
                emitter.complete();
                return emitter;
            }

            // 3. 流式推送 LLM 导语（逐 token）
            StringBuilder acc = new StringBuilder();
            llmService.chatStream(prompt)
                    .doOnNext(token -> {
                        acc.append(token);
                        try {
                            emitter.send(SseEmitter.event().name("reason").data(token));
                        } catch (IOException ignore) {
                            // 客户端断开等，忽略
                        }
                    })
                    .doOnError(e -> {
                        log.warn("推荐导语 LLM 流失败，降级静态文案: {}", e.getMessage());
                        try {
                            if (acc.length() == 0) {
                                emitter.send(SseEmitter.event().name("reason")
                                        .data("推荐导语生成遇到问题，以上书目基于您的借阅历史精选。"));
                            }
                            emitter.send(SseEmitter.event().name("done").data(""));
                            emitter.complete();
                        } catch (IOException ignore) {
                        }
                    })
                    .doOnComplete(() -> {
                        try {
                            emitter.send(SseEmitter.event().name("done").data(""));
                            emitter.complete();
                        } catch (IOException ignore) {
                        }
                    })
                    .subscribe();
        } catch (Exception e) {
            log.error("推荐 SSE 流初始化失败: userId={}", userId, e);
            emitter.completeWithError(e);
        }
        return emitter;
    }
}
