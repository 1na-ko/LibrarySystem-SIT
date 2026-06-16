package com.library.ai.nlp;

import java.util.List;

/**
 * 本地 NLP 服务接口.
 * <p>
 * 基于 HanLP portable 提供中文分词和关键词提取能力，纯本地运行，无网络依赖。
 * 即使未配置任何外部 API Key，此服务也可正常使用。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public interface NlpService {

    /**
     * 中文分词.
     * <p>
     * 使用 HanLP 标准分词器对输入文本进行切分，返回按原文顺序排列的词语列表。
     *
     * @param text 待分词文本（为空时返回空列表）
     * @return 分词结果列表
     */
    List<String> tokenize(String text);

    /**
     * TextRank 关键词提取.
     * <p>
     * 基于 TextRank 算法从文本中提取关键词，按权重降序排列。
     *
     * @param text 待提取文本（为空时返回空列表）
     * @param topK 期望返回的关键词数量（≤ 1 时返回空列表）
     * @return 按权重降序排列的关键词列表
     */
    List<String> extractKeywords(String text, int topK);
}
