package com.library.android.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * 谈判建议.
 */
public class NegotiationSuggestion {

    @SerializedName("resourceName")
    private String resourceName;

    @SerializedName("supplierName")
    private String supplierName;

    @SerializedName("priceRange")
    private PriceRange priceRange;

    @SerializedName("strategies")
    private List<Strategy> strategies;

    @SerializedName("keyTerms")
    private List<String> keyTerms;

    @SerializedName("riskWarnings")
    private List<String> riskWarnings;

    public String getResourceName() { return resourceName; }
    public String getSupplierName() { return supplierName; }
    public PriceRange getPriceRange() { return priceRange; }
    public List<Strategy> getStrategies() { return strategies; }
    public List<String> getKeyTerms() { return keyTerms; }
    public List<String> getRiskWarnings() { return riskWarnings; }

    public static class PriceRange {
        @SerializedName("floorPrice")
        private double floorPrice;

        @SerializedName("ceilingPrice")
        private double ceilingPrice;

        @SerializedName("medianPrice")
        private double medianPrice;

        @SerializedName("suggestedOffer")
        private double suggestedOffer;

        public double getFloorPrice() { return floorPrice; }
        public double getCeilingPrice() { return ceilingPrice; }
        public double getMedianPrice() { return medianPrice; }
        public double getSuggestedOffer() { return suggestedOffer; }
    }

    public static class Strategy {
        @SerializedName("code")
        private String code;

        @SerializedName("description")
        private String description;

        @SerializedName("priority")
        private int priority;

        public String getCode() { return code; }
        public String getDescription() { return description; }
        public int getPriority() { return priority; }
    }
}