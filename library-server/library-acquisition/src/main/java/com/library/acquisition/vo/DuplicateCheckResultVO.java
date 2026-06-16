package com.library.acquisition.vo;

import com.library.acquisition.enums.MatchStrategyEnum;
import com.library.core.vo.BookSimpleVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DuplicateCheckResultVO {
    private boolean isDuplicate;
    private List<DuplicateItem> duplicates;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DuplicateItem {
        private BookSimpleVO book;
        private double score;
        private MatchStrategyEnum matchStrategy;
    }
}
