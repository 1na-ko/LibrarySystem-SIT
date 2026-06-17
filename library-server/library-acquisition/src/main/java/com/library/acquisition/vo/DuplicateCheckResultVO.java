package com.library.acquisition.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    /**
     * 是否检测到重复.
     * <p>
     * 必须使用 {@code @JsonProperty("isDuplicate")} 显式指定字段名：Lombok 为 {@code boolean isDuplicate}
     * 字段生成的 getter 是 {@code isDuplicate()}，Jackson 默认会剥离 {@code is} 前缀序列化为 JSON
     * 字段 {@code duplicate}，与 OpenAPI 契约 {@code isDuplicate} 不一致，前端反序列化会丢失该字段。
     */
    @JsonProperty("isDuplicate")
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
