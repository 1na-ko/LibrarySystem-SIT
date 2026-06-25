package com.library.android.model;

import com.google.gson.annotations.SerializedName;

/**
 * 采购预测结果.
 */
public class PurchasePredictionVO {

    @SerializedName("subjectId")
    private long subjectId;

    @SerializedName("subjectName")
    private String subjectName;

    @SerializedName("month")
    private String month;

    @SerializedName("predictedDemand")
    private int predictedDemand;

    @SerializedName("confidence")
    private double confidence;

    @SerializedName("features")
    private Features features;

    /** WP-0/4：数据不足等降级场景的友好提示（非空时前端展示，不再是"服务不可用"误导）. */
    @SerializedName("message")
    private String message;

    public long getSubjectId() { return subjectId; }
    public String getSubjectName() { return subjectName; }
    public String getMonth() { return month; }
    public int getPredictedDemand() { return predictedDemand; }
    public double getConfidence() { return confidence; }
    public Features getFeatures() { return features; }
    public String getMessage() { return message; }

    public static class Features {
        @SerializedName("historyTrend")
        private double historyTrend;

        @SerializedName("seasonFactor")
        private double seasonFactor;

        @SerializedName("reservationHeat")
        private int reservationHeat;

        public double getHistoryTrend() { return historyTrend; }
        public double getSeasonFactor() { return seasonFactor; }
        public int getReservationHeat() { return reservationHeat; }
    }
}