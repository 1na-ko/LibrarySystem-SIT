package com.library.acquisition.dto.llm;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * LLM JSON Mode 输出 — 谈判建议（仅策略/条款/风险，不含价格维度）.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LlmNegotiationSuggestion {
    private List<Strategy> strategies;
    private List<String> keyTerms;
    private List<String> riskWarnings;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Strategy {
        private String code;
        private String description;
        private int priority;
    }
}
