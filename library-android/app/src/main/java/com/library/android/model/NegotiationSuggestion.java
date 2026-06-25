package com.library.android.model;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;
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

    /** WP2.6：金额字段统一使用 BigDecimal 保证金融精度. */
    public static class PriceRange {
        @SerializedName("floorPrice")
        private BigDecimal floorPrice;

        @SerializedName("ceilingPrice")
        private BigDecimal ceilingPrice;

        @SerializedName("medianPrice")
        private BigDecimal medianPrice;

        @SerializedName("suggestedOffer")
        private BigDecimal suggestedOffer;

        public BigDecimal getFloorPrice() { return floorPrice; }
        public BigDecimal getCeilingPrice() { return ceilingPrice; }
        public BigDecimal getMedianPrice() { return medianPrice; }
        public BigDecimal getSuggestedOffer() { return suggestedOffer; }
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