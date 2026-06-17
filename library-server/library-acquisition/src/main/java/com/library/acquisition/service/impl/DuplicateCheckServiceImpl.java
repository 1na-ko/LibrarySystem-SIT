package com.library.acquisition.service.impl;

import com.library.acquisition.config.AcquisitionProperties;
import com.library.acquisition.enums.MatchStrategyEnum;
import com.library.acquisition.service.DuplicateCheckService;
import com.library.acquisition.vo.DuplicateCheckResultVO;
import com.library.ai.nlp.NlpService;
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
        List<DuplicateCheckResultVO.DuplicateItem> candidates = new ArrayList<>();
        Set<String> seenIsbn = new HashSet<>();

        // 策略1: ISBN 精确匹配（权重 1.0）
        if (StringUtils.hasText(isbn)) {
            Book existed = bookMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Book>()
                            .eq(Book::getIsbn, isbn)
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

        // 策略3: 标题模糊匹配（ES 不可用时的降级——按借阅次数排序，优先匹配热门图书）
        if (StringUtils.hasText(title) && candidates.isEmpty()) {
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
