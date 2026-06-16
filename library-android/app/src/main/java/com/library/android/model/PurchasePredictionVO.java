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

    public long getSubjectId() { return subjectId; }
    public String getSubjectName() { return subjectName; }
    public String getMonth() { return month; }
    public int getPredictedDemand() { return predictedDemand; }
    public double getConfidence() { return confidence; }
    public Features getFeatures() { return features; }

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