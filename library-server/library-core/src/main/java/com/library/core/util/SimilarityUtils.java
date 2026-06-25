package com.library.core.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 向量与集合相似度计算工具（包内可见）.
 * <p>
 * 为协同过滤和内容推荐提供余弦相似度、Jaccard 相似度、归一化等基础计算。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public final class SimilarityUtils {

    private SimilarityUtils() {
    }

    /**
     * 计算两个集合的 Jaccard 相似系数.
     *
     * @param a 集合 A
     * @param b 集合 B
     * @return Jaccard ∈ [0, 1]，两个空集合返回 0
     */
    public static <T> double jaccard(Set<T> a, Set<T> b) {
        if (a.isEmpty() && b.isEmpty()) {
            return 0.0;
        }
        Set<T> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        Set<T> union = new HashSet<>(a);
        union.addAll(b);
        return (double) intersection.size() / union.size();
    }

    /**
     * 计算两个浮点向量的余弦相似度.
     *
     * @param a 向量 A
     * @param b 向量 B
     * @return 余弦相似度 ∈ [-1, 1]，任一空向量返回 0
     */
    public static double cosine(List<Float> a, List<Float> b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty() || a.size() != b.size()) {
            return 0.0;
        }
        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.size(); i++) {
            double va = a.get(i);
            double vb = b.get(i);
            dot += va * vb;
            normA += va * va;
            normB += vb * vb;
        }
        double denominator = Math.sqrt(normA) * Math.sqrt(normB);
        if (denominator == 0.0) {
            return 0.0;
        }
        return dot / denominator;
    }

    /**
     * 计算两个集合的余弦相似度（基于共同元素）.
     * <p>
     * 将两个集合视为二值向量：cosine = |A∩B| / sqrt(|A| * |B|).
     *
     * @param a 集合 A
     * @param b 集合 B
     * @return 余弦相似度 ∈ [0, 1]
     */
    public static <T> double setCosine(Set<T> a, Set<T> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }
        Set<T> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        return intersection.size() / Math.sqrt((double) a.size() * b.size());
    }

    /**
     * 归一化分数到 [0, 1] 范围.
     *
     * @param scores 原始分数 Map
     * @return 归一化后的分数 Map（max=1.0），全相同分数时各保留原值
     */
    public static Map<Long, Double> normalize(Map<Long, Double> scores) {
        if (scores.isEmpty()) {
            return Collections.emptyMap();
        }
        double max = scores.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        if (max == 0.0) {
            return scores;
        }
        return scores.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue() / max));
    }

    /**
     * 将分数按权重合并到目标 Map.
     *
     * @param target 目标 Map（会被修改）
     * @param source 来源分数
     * @param weight 权重
     */
    public static void mergeWithWeight(Map<Long, Double> target, Map<Long, Double> source, double weight) {
        source.forEach((id, score) -> target.merge(id, score * weight, Double::sum));
    }
}
