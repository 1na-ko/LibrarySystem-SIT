package com.library.acquisition.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchasePredictionVO {
    private Long subjectId;
    private String subjectName;
    private String month;
    private int predictedDemand;
    private double confidence;
    private FeatureMap features;
    /** WP-0：数据不足等降级场景的友好提示（非空时前端展示）. */
    private String message;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeatureMap {
        private double historyTrend;
        private double seasonFactor;
        private int reservationHeat;
    }
}
