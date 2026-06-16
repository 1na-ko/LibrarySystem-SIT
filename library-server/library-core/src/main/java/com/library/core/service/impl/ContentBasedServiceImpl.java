package com.library.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.ai.embedding.EmbeddingService;
import com.library.core.config.RecommendationProperties;
import com.library.core.entity.Book;
import com.library.core.entity.BorrowRecord;
import com.library.core.entity.Category;
import com.library.core.mapper.BookMapper;
import com.library.core.mapper.BorrowRecordMapper;
import com.library.core.mapper.CategoryMapper;
import com.library.core.service.ContentBasedService;
import com.library.core.util.SimilarityUtils;
import com.library.core.vo.BookSimpleVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 基于内容的推荐服务实现.
 * <p>
 * EmbeddingService 条件注入（{@code @Autowired(required = false)}），
 * API Key 缺失时内容推荐路静默返回空，不阻塞其他推荐路径。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class ContentBasedServiceImpl implements ContentBasedService {

    private final BookMapper bookMapper;
    private final BorrowRecordMapper borrowRecordMapper;
    private final CategoryMapper categoryMapper;
    private final RecommendationProperties properties;

    /** EmbeddingService 可能因 API Key 缺失而不存在 */
    @Nullable
    private final EmbeddingService embeddingService;

    public ContentBasedServiceImpl(
            BookMapper bookMapper,
            BorrowRecordMapper borrowRecordMapper,
            CategoryMapper categoryMapper,
            RecommendationProperties properties,
            @Autowired(required = false) EmbeddingService embeddingService) {
        this.bookMapper = bookMapper;
        this.borrowRecordMapper = borrowRecordMapper;
        this.categoryMapper = categoryMapper;
        this.properties = properties;
        this.embeddingService = embeddingService;
    }

    @Override
    public Map<Long, Double> recommend(Long userId, int topN) {
        if (embeddingService == null) {
            log.debug("ContentBased: EmbeddingService 未就绪，跳过内容推荐");
            return Collections.emptyMap();
        }
        // 独立调用入口：自行加载用户已借阅 ID
        List<BorrowRecord> allRecords = borrowRecordMapper.selectAllActiveForCF();
        Set<Long> borrowedBookIds = allRecords.stream()
                .filter(r -> r.getUserId().equals(userId))
                .map(BorrowRecord::getBookId)
                .collect(Collectors.toSet());
        return recommend(userId, borrowedBookIds, topN);
    }

    @Override
    public Map<Long, Double> recommend(Long userId, Set<Long> borrowedBookIds, int topN) {
        if (embeddingService == null) {
            log.debug("ContentBased: EmbeddingService 未就绪，跳过内容推荐");
            return Collections.emptyMap();
        }
        if (borrowedBookIds == null || borrowedBookIds.isEmpty()) {
            log.debug("ContentBased: 用户 {} 无借阅记录，跳过", userId);
            return Collections.emptyMap();
        }

        // 2. 批量查询已借图书
        List<Book> borrowedBooks = bookMapper.selectBatchIds(borrowedBookIds);
        if (borrowedBooks.isEmpty()) {
            return Collections.emptyMap();
        }

        // 3. 构建文本并批量向量化
        List<String> borrowedTexts = borrowedBooks.stream()
                .map(ContentBasedServiceImpl::buildText)
                .collect(Collectors.toList());
        List<List<Float>> borrowedVectors = embeddingService.batchEmbed(borrowedTexts);

        // 4. 计算用户画像向量（逐元素均值）
        List<Float> userProfile = computeMeanVector(borrowedVectors);

        // 5. 获取候选图书（按 borrowCount 降序，排除已借）
        int candidateLimit = properties.getContentCandidateLimit();
        List<Book> candidates = bookMapper.selectList(
                new LambdaQueryWrapper<Book>()
                        .notIn(Book::getId, borrowedBookIds)
                        .orderByDesc(Book::getBorrowCount)
                        .last("LIMIT " + candidateLimit)
        );

        if (candidates.isEmpty()) {
            return Collections.emptyMap();
        }

        // 6. 候选图书批量向量化
        List<String> candidateTexts = candidates.stream()
                .map(ContentBasedServiceImpl::buildText)
                .collect(Collectors.toList());
        List<List<Float>> candidateVectors = embeddingService.batchEmbed(candidateTexts);

        // 7. 计算余弦相似度
        Map<Long, Double> scores = new HashMap<>();
        for (int i = 0; i < candidates.size(); i++) {
            double cosine = SimilarityUtils.cosine(userProfile, candidateVectors.get(i));
            // 将 [-1, 1] 映射到 [0, 1]
            double normalized = (cosine + 1.0) / 2.0;
            scores.put(candidates.get(i).getId(), normalized);
        }

        // 8. 取 Top-N
        Map<Long, Double> topScores = scores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(topN)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, HashMap::new));

        log.debug("ContentBased: userId={}, 候选池={}, Top-{}={}",
                userId, candidates.size(), topN, topScores.size());
        return topScores;
    }

    /**
     * 构建图书文本表示（标题 + 关键词）.
     */
    private static String buildText(Book book) {
        StringBuilder sb = new StringBuilder(book.getTitle() != null ? book.getTitle() : "");
        if (book.getKeywords() != null && !book.getKeywords().isEmpty()) {
            sb.append(" ").append(book.getKeywords());
        }
        return sb.toString();
    }

    /**
     * 计算向量列表的逐元素均值.
     */
    private static List<Float> computeMeanVector(List<List<Float>> vectors) {
        if (vectors.isEmpty()) {
            return Collections.emptyList();
        }
        int dim = vectors.get(0).size();
        List<Float> mean = new ArrayList<>(dim);
        for (int i = 0; i < dim; i++) {
            float sum = 0.0f;
            for (List<Float> vec : vectors) {
                sum += vec.get(i);
            }
            mean.add(sum / vectors.size());
        }
        return mean;
    }

    /**
     * Book → BookSimpleVO（含分类名称，避免 N+1）.
     */
    BookSimpleVO toBookSimpleVO(Book book, Map<Long, String> categoryNameMap) {
        return BookSimpleVO.from(book, categoryNameMap.get(book.getCategoryId()));
    }
}
