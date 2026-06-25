package com.library.acquisition.service.impl;

import com.library.acquisition.config.AcquisitionProperties;
import com.library.acquisition.enums.MatchStrategyEnum;
import com.library.acquisition.service.DuplicateCheckService;
import com.library.acquisition.vo.DuplicateCheckResultVO;
import com.library.ai.nlp.NlpService;
import com.library.common.exception.BizException;
import com.library.common.exception.ErrorCode;
import com.library.core.entity.Book;
import com.library.core.mapper.BookMapper;
import com.library.core.vo.BookSimpleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class DuplicateCheckServiceImpl implements DuplicateCheckService {

    private final BookMapper bookMapper;
    private final NlpService nlpService;
    private final AcquisitionProperties props;

    @Override
    public DuplicateCheckResultVO checkDuplicate(String isbn, String title, String author) {
        // isbn / title 至少提供一个，否则无查重依据
        if (!StringUtils.hasText(isbn) && !StringUtils.hasText(title)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "请至少提供 ISBN 或书名进行查重");
        }
        List<DuplicateCheckResultVO.DuplicateItem> candidates = new ArrayList<>();
        Set<String> seenIsbn = new HashSet<>();

        // 策略1: ISBN 精确匹配（权重 1.0）— 归一化（去连字符/空格）后匹配，兼容多种输入格式
        if (StringUtils.hasText(isbn)) {
            String normalizedIsbn = isbn.replaceAll("[^0-9Xx]", "");
            Book existed = bookMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Book>()
                            .apply("REPLACE(isbn,'-','') = {0}", normalizedIsbn)
                            .eq(Book::getDeleted, 0));
            if (existed != null) {
                candidates.add(DuplicateCheckResultVO.DuplicateItem.builder()
                        .book(toSimpleVO(existed))
                        .score(1.0)
                        .matchStrategy(MatchStrategyEnum.ISBN_EXACT)
                        .build());
                seenIsbn.add(isbn);
            }
        }

        // 策略2: 作者+标题联合匹配
        // 预计算输入标题的分词 + 频率 Map（循环外复用，消除 O(n) 次重复 NLP 调用）
        List<String> inputTokens = StringUtils.hasText(title) ? nlpService.tokenize(title) : List.of();
        Map<String, Long> inputFreq = buildFreqMap(inputTokens);

        if (StringUtils.hasText(author) && StringUtils.hasText(title)) {
            List<Book> authorBooks = bookMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Book>()
                            .eq(Book::getAuthor, author)
                            .eq(Book::getDeleted, 0));
            for (Book book : authorBooks) {
                if (book.getIsbn() != null && seenIsbn.contains(book.getIsbn())) continue;
                double sim = cosineSimilarity(inputTokens, inputFreq, book.getTitle());
                if (sim > props.getDuplicateAuthorTitleThreshold()) {
                    candidates.add(DuplicateCheckResultVO.DuplicateItem.builder()
                            .book(toSimpleVO(book))
                            .score(sim * MatchStrategyEnum.AUTHOR_TITLE.getWeight())
                            .matchStrategy(MatchStrategyEnum.AUTHOR_TITLE)
                            .build());
                    if (book.getIsbn() != null) seenIsbn.add(book.getIsbn());
                }
            }
        }

        // 策略3: 标题模糊匹配（ES 不可用时的降级）
        // WP-0：先做 LIKE 前置快速路径——绕过 HanLP 短标题分词不稳定（如"红楼梦"被拆为单字致余弦≈0），
        // 命中即返回高分；不命中再走原 Top-200 余弦匹配兜底
        if (StringUtils.hasText(title) && candidates.isEmpty()) {
            // 先 LIKE %title% 精确包含匹配
            List<Book> likeBooks = bookMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Book>()
                            .like(Book::getTitle, title)
                            .eq(Book::getDeleted, 0)
                            .orderByDesc(Book::getBorrowCount)
                            .last("LIMIT 20"));
            for (Book book : likeBooks) {
                if (book.getIsbn() != null && seenIsbn.contains(book.getIsbn())) continue;
                // 标题包含查询关键词：高度可能为重复
                // 完全相等给 1.0；包含给 0.95（再乘以 TITLE_FUZZY 权重 0.85 = 0.808 仍 ≥ 0.7 阈值）
                double sim = title.equals(book.getTitle()) ? 1.0 : 0.95;
                candidates.add(DuplicateCheckResultVO.DuplicateItem.builder()
                        .book(toSimpleVO(book))
                        .score(sim * MatchStrategyEnum.TITLE_FUZZY.getWeight())
                        .matchStrategy(MatchStrategyEnum.TITLE_FUZZY)
                        .build());
                if (book.getIsbn() != null) seenIsbn.add(book.getIsbn());
            }

            // LIKE 未命中再走 Top-200 余弦匹配兜底（处理"java并发"vs"Java 并发编程"等分词差异场景）
            if (candidates.isEmpty()) {
                List<Book> allBooks = bookMapper.selectList(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Book>()
                                .eq(Book::getDeleted, 0)
                                .orderByDesc(Book::getBorrowCount)
                                .last("LIMIT 200"));
                for (Book book : allBooks) {
                    if (book.getIsbn() != null && seenIsbn.contains(book.getIsbn())) continue;
                    double sim = cosineSimilarity(inputTokens, inputFreq, book.getTitle());
                    if (sim > props.getDuplicateTitleThreshold()) {
                        candidates.add(DuplicateCheckResultVO.DuplicateItem.builder()
                                .book(toSimpleVO(book))
                                .score(sim * MatchStrategyEnum.TITLE_FUZZY.getWeight())
                                .matchStrategy(MatchStrategyEnum.TITLE_FUZZY)
                                .build());
                    }
                }
            }
        }

        boolean isDuplicate = candidates.stream().anyMatch(c -> c.getScore() >= 0.7);
        return DuplicateCheckResultVO.builder()
                .isDuplicate(isDuplicate)
                .duplicates(candidates)
                .build();
    }

    /**
     * 余弦相似度（接受预计算的输入侧分词和频率 Map，避免循环内重复 NLP 调用）.
     */
    private double cosineSimilarity(List<String> tokensA, Map<String, Long> freqA, String b) {
        if (tokensA.isEmpty() || b == null) return 0.0;
        List<String> tokensB = nlpService.tokenize(b);
        if (tokensB.isEmpty()) return 0.0;
        Map<String, Long> freqB = buildFreqMap(tokensB);

        Set<String> vocab = new HashSet<>();
        vocab.addAll(tokensA);
        vocab.addAll(tokensB);
        double dot = 0, normA = 0, normB = 0;
        for (String t : vocab) {
            long ca = freqA.getOrDefault(t, 0L);
            long cb = freqB.getOrDefault(t, 0L);
            dot += ca * cb;
            normA += ca * ca;
            normB += cb * cb;
        }
        if (normA == 0 || normB == 0) return 0.0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private Map<String, Long> buildFreqMap(List<String> tokens) {
        Map<String, Long> freq = new HashMap<>();
        for (String t : tokens) {
            freq.merge(t, 1L, Long::sum);
        }
        return freq;
    }

    private BookSimpleVO toSimpleVO(Book book) {
        return BookSimpleVO.builder()
                .id(book.getId())
                .isbn(book.getIsbn())
                .title(book.getTitle())
                .author(book.getAuthor())
                .publisher(book.getPublisher())
                .coverUrl(book.getCoverUrl())
                .pubDate(book.getPubDate())
                .availCopies(book.getAvailCopies())
                .build();
    }
}
