package com.library.core.service.impl;

import com.library.ai.llm.LlmService;
import com.library.ai.llm.LlmUnavailableException;
import com.library.common.exception.BizException;
import com.library.common.exception.ErrorCode;
import com.library.core.config.RecommendationProperties;
import com.library.core.entity.Book;
import com.library.core.entity.BorrowRecord;
import com.library.core.entity.Category;
import com.library.core.mapper.BookMapper;
import com.library.core.mapper.BorrowRecordMapper;
import com.library.core.mapper.CategoryMapper;
import com.library.core.service.CollaborativeFilteringService;
import com.library.core.service.ContentBasedService;
import com.library.core.service.KGBasedRecommendService;
import com.library.core.service.RecommendationService;
import com.library.core.util.SimilarityUtils;
import com.library.core.vo.BookRecommendVO;
import com.library.core.vo.BookSimpleVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * 推荐引擎编排服务实现.
 * <p>
 * 多路召回（CF + Content + KG）并行执行，加权融合后精排 Top-N，
 * 最后通过 LLM 生成个性化推荐理由（LLM 不可用时降级为模板）。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class RecommendationServiceImpl implements RecommendationService {

    private final CollaborativeFilteringService cfService;
    private final ContentBasedService contentBasedService;
    private final KGBasedRecommendService kgService;
    private final BookMapper bookMapper;
    private final BorrowRecordMapper borrowRecordMapper;
    private final CategoryMapper categoryMapper;
    private final RecommendationProperties properties;

    /** 推荐并行召回专用线程池（AsyncConfig.taskExecutor，与 ForkJoinPool.commonPool 隔离） */
    private final Executor recommendExecutor;

    /** LLM 服务可能因 API Key 缺失而不存在 */
    @org.springframework.lang.Nullable
    private final LlmService llmService;

    /** 推荐理由模板池 */
    private static final String[] REASON_TEMPLATES = {
            "与您借阅偏好相似的读者也喜欢此书",
            "本书主题与您的阅读偏好紧密相关",
            "基于您的借阅历史，为您精选推荐",
            "本书内容与您感兴趣的领域高度契合",
            "根据您的阅读偏好，这本书值得一读"
    };

    public RecommendationServiceImpl(
            CollaborativeFilteringService cfService,
            ContentBasedService contentBasedService,
            KGBasedRecommendService kgService,
            BookMapper bookMapper,
            BorrowRecordMapper borrowRecordMapper,
            CategoryMapper categoryMapper,
            RecommendationProperties properties,
            @Qualifier("taskExecutor") Executor recommendExecutor,
            @org.springframework.beans.factory.annotation.Autowired(required = false) LlmService llmService) {
        this.cfService = cfService;
        this.contentBasedService = contentBasedService;
        this.kgService = kgService;
        this.bookMapper = bookMapper;
        this.borrowRecordMapper = borrowRecordMapper;
        this.categoryMapper = categoryMapper;
        this.properties = properties;
        this.recommendExecutor = recommendExecutor;
        this.llmService = llmService;
    }

    @Override
    public List<BookRecommendVO> recommend(Long userId, int limit) {
        int actualLimit = Math.max(1, Math.min(limit, properties.getMaxLimit()));
        long timeout = properties.getRecallTimeoutSeconds();

        // 0. 顶层一次性加载全量活跃借阅记录，分发给各召回路径，避免重复全表扫描
        List<BorrowRecord> allRecords = borrowRecordMapper.selectAllActiveForCF();
        Set<Long> borrowedBookIds = allRecords.stream()
                .filter(r -> r.getUserId().equals(userId))
                .map(BorrowRecord::getBookId)
                .collect(Collectors.toSet());

        // 1. 并行多路召回（复用预加载数据，使用隔离线程池）
        Map<Long, Double> cfResult = Collections.emptyMap();
        Map<Long, Double> cbfResult = Collections.emptyMap();
        Map<Long, Double> kgResult = Collections.emptyMap();

        try {
            CompletableFuture<Map<Long, Double>> cfFuture = CompletableFuture.supplyAsync(
                    () -> cfService.recommend(userId, allRecords), recommendExecutor);
            CompletableFuture<Map<Long, Double>> cbfFuture = CompletableFuture.supplyAsync(
                    () -> contentBasedService.recommend(userId, borrowedBookIds, actualLimit), recommendExecutor);
            CompletableFuture<Map<Long, Double>> kgFuture = CompletableFuture.supplyAsync(
                    () -> kgService.recommend(userId, actualLimit), recommendExecutor);

            CompletableFuture<Void> allFutures = CompletableFuture.allOf(cfFuture, cbfFuture, kgFuture);
            allFutures.get(timeout, TimeUnit.SECONDS);

            cfResult = cfFuture.getNow(Collections.emptyMap());
            cbfResult = cbfFuture.getNow(Collections.emptyMap());
            kgResult = kgFuture.getNow(Collections.emptyMap());
        } catch (TimeoutException e) {
            log.warn("推荐并行召回超时（{}s），使用已完成路径的部分结果", timeout);
        } catch (Exception e) {
            log.warn("推荐并行召回异常: {}", e.getMessage(), e);
        }

        // 2. 加权融合
        Map<Long, Double> fused = new HashMap<>();
        SimilarityUtils.mergeWithWeight(fused, cfResult, properties.getCfWeight());
        SimilarityUtils.mergeWithWeight(fused, cbfResult, properties.getContentWeight());
        SimilarityUtils.mergeWithWeight(fused, kgResult, properties.getKgWeight());

        // 3. 排除已借阅图书（复用顶层加载的集合）
        borrowedBookIds.forEach(fused::remove);

        if (fused.isEmpty()) {
            log.debug("推荐: userId={}, 无候选图书", userId);
            return Collections.emptyList();
        }

        // 4. 精排 Top-N
        List<Map.Entry<Long, Double>> topN = fused.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(actualLimit)
                .collect(Collectors.toList());

        // 5. 批量加载图书 + 分类名称
        List<Long> topBookIds = topN.stream().map(Map.Entry::getKey).collect(Collectors.toList());
        List<Book> topBooks = bookMapper.selectBatchIds(topBookIds);
        Map<Long, String> categoryNameMap = loadCategoryNames(topBooks);

        // 按分数排序（因为 selectBatchIds 不保证顺序）
        Map<Long, Book> bookMap = topBooks.stream()
                .collect(Collectors.toMap(Book::getId, b -> b));
        Map<Long, Double> scoreMap = topN.stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // 6. 生成推荐理由（复用顶层加载的 borrowedBookIds）
        List<String> reasons = generateReasons(userId, borrowedBookIds, topBooks, scoreMap, categoryNameMap);

        // 7. 组装 VO
        List<BookRecommendVO> result = new ArrayList<>();
        int idx = 0;
        for (Map.Entry<Long, Double> entry : topN) {
            Book book = bookMap.get(entry.getKey());
            if (book == null) {
                continue;
            }
            String reason = idx < reasons.size() ? reasons.get(idx) : REASON_TEMPLATES[0];
            result.add(BookRecommendVO.builder()
                    .book(toBookSimpleVO(book, categoryNameMap))
                    .score(Math.round(entry.getValue() * 1000.0) / 1000.0) // 保留 3 位小数
                    .reason(reason)
                    .build());
            idx++;
        }

        log.debug("推荐: userId={}, 候选融合={}, Top-{}={}", userId, fused.size(), actualLimit, result.size());
        return result;
    }

    /**
     * 生成推荐理由：优先 LLM，降级模板.
     *
     * @param borrowedBookIds 用户已借阅图书 ID（顶层预加载，避免重复全表扫描）
     */
    private List<String> generateReasons(Long userId, Set<Long> borrowedBookIds, List<Book> topBooks,
                                          Map<Long, Double> scoreMap,
                                          Map<Long, String> categoryNameMap) {
        if (llmService == null || topBooks.isEmpty()) {
            return templateReasons(topBooks.size());
        }

        try {
            List<Book> borrowedBooks = borrowedBookIds.isEmpty()
                    ? Collections.emptyList()
                    : bookMapper.selectBatchIds(new ArrayList<>(borrowedBookIds));
            List<String> borrowedTitles = borrowedBooks.stream()
                    .map(Book::getTitle)
                    .limit(10)
                    .collect(Collectors.toList());

            // 构建推荐图书描述
            List<String> recDescriptions = new ArrayList<>();
            for (Book book : topBooks) {
                recDescriptions.add(String.format("《%s》(%s)",
                        book.getTitle(), book.getAuthor() != null ? book.getAuthor() : "未知作者"));
            }

            String prompt = String.format(
                    "你是高校图书馆推荐助手。请根据用户借阅历史，为以下推荐图书各生成一句中文推荐理由（15-25字）。\n" +
                            "用户已借阅: %s\n推荐图书: %s\n" +
                            "请以 JSON 数组格式返回，每项含 bookId(数字) 和 reason(字符串) 两个字段。",
                    borrowedTitles, String.join(", ", recDescriptions));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> llmResult = llmService.chat(prompt, List.class);

            // 构建 bookId → reason 映射
            Map<Long, String> reasonMap = new HashMap<>();
            if (llmResult != null) {
                for (Map<String, Object> item : llmResult) {
                    try {
                        Object bookIdObj = item.get("bookId");
                        Long bookId = bookIdObj instanceof Number
                                ? ((Number) bookIdObj).longValue() : Long.valueOf(String.valueOf(bookIdObj));
                        String reason = (String) item.get("reason");
                        if (bookId != null && reason != null) {
                            reasonMap.put(bookId, reason);
                        }
                    } catch (Exception e) {
                        log.debug("解析 LLM 推荐理由失败: {}", item);
                    }
                }
            }

            // 按 topBooks 顺序组装理由列表
            List<String> reasons = new ArrayList<>();
            for (Book book : topBooks) {
                reasons.add(reasonMap.getOrDefault(book.getId(),
                        "本书与您的阅读偏好高度匹配，推荐阅读"));
            }
            return reasons;

        } catch (LlmUnavailableException e) {
            log.warn("LLM 不可用，降级为模板推荐理由: {}", e.getMessage());
            return templateReasons(topBooks.size());
        } catch (Exception e) {
            log.warn("LLM 推荐理由生成失败，降级: {}", e.getMessage());
            return templateReasons(topBooks.size());
        }
    }

    /**
     * 模板理由（降级）.
     */
    private List<String> templateReasons(int count) {
        List<String> reasons = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            reasons.add(REASON_TEMPLATES[i % REASON_TEMPLATES.length]);
        }
        return reasons;
    }

    /**
     * 批量加载分类名称映射.
     */
    private Map<Long, String> loadCategoryNames(List<Book> books) {
        List<Long> categoryIds = books.stream()
                .map(Book::getCategoryId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        if (categoryIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return categoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
    }

    /**
     * Book → BookSimpleVO（避免 N+1 分类查询）.
     */
    private BookSimpleVO toBookSimpleVO(Book book, Map<Long, String> categoryNameMap) {
        return BookSimpleVO.from(book, categoryNameMap.get(book.getCategoryId()));
    }
}
