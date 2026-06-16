package com.library.acquisition.vo;

import com.library.acquisition.dto.PriceRangeDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NegotiationSuggestionVO {
    private String resourceName;
    private String supplierName;
    private PriceRangeDTO priceRange;
    private List<StrategyItem> strategies;
    private List<String> keyTerms;
    private List<String> riskWarnings;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StrategyItem {
        private String code;
        private String description;
        private int priority;
    }
}
