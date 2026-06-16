package com.library.core.service.impl;

import com.library.core.config.RecommendationProperties;
import com.library.core.entity.BorrowRecord;
import com.library.core.mapper.BorrowRecordMapper;
import com.library.core.service.CollaborativeFilteringService;
import com.library.core.util.SimilarityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 协同过滤推荐服务实现.
 * <p>
 * 基于全量活跃借阅记录在内存中构建用户-图书交互矩阵。
 * User-CF 使用余弦相似度找相似用户，Item-CF 使用 Jaccard 找相似图书。
 * 两者以内部权重融合（User-CF 0.6, Item-CF 0.4）后返回归一化分数。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CollaborativeFilteringServiceImpl implements CollaborativeFilteringService {

    private final BorrowRecordMapper borrowRecordMapper;
    private final RecommendationProperties properties;

    /** User-CF 与 Item-CF 内部融合权重 */
    private static final double USER_CF_WEIGHT = 0.6;
    private static final double ITEM_CF_WEIGHT = 0.4;

    @Override
    public Map<Long, Double> recommend(Long userId) {
        // 独立调用入口：自行全量加载（测试/单路调用场景）
        return recommend(userId, borrowRecordMapper.selectAllActiveForCF());
    }

    @Override
    public Map<Long, Double> recommend(Long userId, List<BorrowRecord> allRecords) {
        // 1. 活跃借阅记录由调用方预加载（推荐编排层统一加载一次，避免多路重复全表扫描）
        if (allRecords == null || allRecords.isEmpty()) {
            log.debug("CF: 无活跃借阅记录，返回空");
            return Collections.emptyMap();
        }

        // 2. 构建 user→Set<bookId> 和 book→Set<userId> 映射
        Map<Long, Set<Long>> userBooks = new HashMap<>();
        Map<Long, Set<Long>> bookUsers = new HashMap<>();

        for (BorrowRecord record : allRecords) {
            long uid = record.getUserId();
            long bid = record.getBookId();
            userBooks.computeIfAbsent(uid, k -> new HashSet<>()).add(bid);
            bookUsers.computeIfAbsent(bid, k -> new HashSet<>()).add(uid);
        }

        Set<Long> targetBooks = userBooks.getOrDefault(userId, Collections.emptySet());
        if (targetBooks.isEmpty()) {
            log.debug("CF: 用户 {} 无借阅记录，返回空", userId);
            return Collections.emptyMap();
        }

        // 3. User-CF
        Map<Long, Double> userCfScores = computeUserCF(userId, targetBooks, userBooks);

        // 4. Item-CF
        Map<Long, Double> itemCfScores = computeItemCF(targetBooks, bookUsers, userBooks.get(userId));

        // 5. 内部融合
        Map<Long, Double> fused = new HashMap<>();
        SimilarityUtils.mergeWithWeight(fused, userCfScores, USER_CF_WEIGHT);
        SimilarityUtils.mergeWithWeight(fused, itemCfScores, ITEM_CF_WEIGHT);

        // 6. 排除已借阅图书
        targetBooks.forEach(fused::remove);

        // 7. 归一化
        Map<Long, Double> normalized = SimilarityUtils.normalize(fused);

        log.debug("CF: userId={}, user-CF候选={}, item-CF候选={}, 融合后={}",
                userId, userCfScores.size(), itemCfScores.size(), normalized.size());
        return normalized;
    }

    /**
     * User-CF：余弦相似度找 Top-K 相似用户，聚合其借阅.
     */
    private Map<Long, Double> computeUserCF(Long userId, Set<Long> targetBooks,
                                            Map<Long, Set<Long>> userBooks) {
        int topK = properties.getUserCfTopK();

        // 计算所有其他用户与目标用户的余弦相似度
        List<Map.Entry<Long, Double>> similarities = userBooks.entrySet().stream()
                .filter(e -> !e.getKey().equals(userId))
                .filter(e -> e.getValue().size() >= 2) // 过滤借阅量过少的用户
                .map(e -> {
                    double sim = SimilarityUtils.setCosine(targetBooks, e.getValue());
                    return Map.entry(e.getKey(), sim);
                })
                .filter(e -> e.getValue() > 0.0)
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(topK)
                .collect(Collectors.toList());

        if (similarities.isEmpty()) {
            return Collections.emptyMap();
        }

        // 聚合相似用户的借阅
        Map<Long, Double> candidates = new HashMap<>();
        for (var entry : similarities) {
            Long similarUserId = entry.getKey();
            double similarity = entry.getValue();
            Set<Long> similarUserBooks = userBooks.get(similarUserId);
            for (Long bookId : similarUserBooks) {
                if (!targetBooks.contains(bookId)) {
                    candidates.merge(bookId, similarity, Double::sum);
                }
            }
        }

        return SimilarityUtils.normalize(candidates);
    }

    /**
     * Item-CF：Jaccard 相似度找相似图书.
     */
    private Map<Long, Double> computeItemCF(Set<Long> targetBooks,
                                            Map<Long, Set<Long>> bookUsers,
                                            Set<Long> borrowedBooks) {
        int topK = properties.getItemCfTopK();

        // 收集与目标用户已借图书相似的所有候选
        Map<Long, Double> allCandidates = new HashMap<>();

        for (Long bookId : targetBooks) {
            Set<Long> usersOfBook = bookUsers.getOrDefault(bookId, Collections.emptySet());
            if (usersOfBook.size() < 2) {
                continue; // 借阅人数过少，跳过
            }

            // 计算此书的相似书（仅与其他借阅记录的书比较）
            List<Map.Entry<Long, Double>> similarBooks = bookUsers.entrySet().stream()
                    .filter(e -> !e.getKey().equals(bookId))
                    .filter(e -> !borrowedBooks.contains(e.getKey()))
                    .filter(e -> e.getValue().size() >= 2)
                    .map(e -> {
                        double jaccard = SimilarityUtils.jaccard(usersOfBook, e.getValue());
                        return Map.entry(e.getKey(), jaccard);
                    })
                    .filter(e -> e.getValue() > 0.0)
                    .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                    .limit(topK)
                    .collect(Collectors.toList());

            for (var entry : similarBooks) {
                // 对同一候选书取最高相似度
                allCandidates.merge(entry.getKey(), entry.getValue(), Double::max);
            }
        }

        return SimilarityUtils.normalize(allCandidates);
    }
}
