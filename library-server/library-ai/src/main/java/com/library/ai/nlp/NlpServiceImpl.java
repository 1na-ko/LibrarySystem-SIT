package com.library.ai.nlp;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;
import com.library.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 本地 NLP 服务实现.
 * <p>
 * 基于 HanLP 1.8.5 portable 提供中文分词和 TextRank 关键词提取。
 * HanLP 模型数据内嵌于 JAR 包中，首次调用时触发懒加载（约 1-2 秒）。
 * 所有方法为纯本地计算，无网络依赖，始终可用。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class NlpServiceImpl implements NlpService {

    /**
     * 中文分词.
     * <p>
     * {@link HanLP#segment(String)} 返回 {@link List}&lt;{@link Term}&gt;，
     * 通过 {@code term.word} 字段提取词语文本。
     *
     * @param text 待分词文本
     * @return 分词词语列表，空输入或异常时返回空列表
     */
    @Override
    public List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }
        try {
            List<Term> terms = HanLP.segment(text);
            return terms.stream()
                    .map(term -> term.word)
                    .toList();
        } catch (Exception e) {
            log.error("HanLP 分词异常: text={}", StringUtils.truncate(text, 100), e);
            return Collections.emptyList();
        }
    }

    /**
     * TextRank 关键词提取.
     * <p>
     * {@link HanLP#extractKeyword(String, int)} 返回按权重降序排列的关键词列表。
     *
     * @param text 待提取文本
     * @param topK 期望返回的关键词数量
     * @return 关键词列表，空输入/topK ≤ 0/异常时返回空列表
     */
    @Override
    public List<String> extractKeywords(String text, int topK) {
        if (text == null || text.isBlank() || topK <= 0) {
            return Collections.emptyList();
        }
        try {
            return HanLP.extractKeyword(text, topK);
        } catch (Exception e) {
            log.error("HanLP 关键词提取异常: topK={}, text={}", topK,
                    StringUtils.truncate(text, 100), e);
            return Collections.emptyList();
        }
    }
}
