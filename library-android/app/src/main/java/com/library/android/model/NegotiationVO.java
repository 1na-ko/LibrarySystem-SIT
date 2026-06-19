package com.library.android.model;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

/**
 * 谈判会话视图 — 对应后端 {@code NegotiationVO}（C.1 智能采编模块新增）.
 *
 * <p>{@link com.library.android.network.LibraryApi#createNegotiation} 返回此类型，
 * 谈判建议由 {@link NegotiationSuggestion} 单独表示（前者是会话元数据，后者是 LLM 建议结果）.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public class NegotiationVO {

    @SerializedName("id")
    private long id;

    @SerializedName("resourceId")
    private long resourceId;

    @SerializedName("supplierId")
    private long supplierId;

    @SerializedName("negotiatorId")
    private long negotiatorId;

    @SerializedName("status")
    private String status;

    @SerializedName("floorPrice")
    private BigDecimal floorPrice;

    @SerializedName("ceilingPrice")
    private BigDecimal ceilingPrice;

    @SerializedName("suggestedOffer")
    private BigDecimal suggestedOffer;

    @SerializedName("createTime")
    private String createTime;

    public long getId() { return id; }
    public long getResourceId() { return resourceId; }
    public long getSupplierId() { return supplierId; }
    public long getNegotiatorId() { return negotiatorId; }
    public String getStatus() { return status; }
    public BigDecimal getFloorPrice() { return floorPrice; }
    public BigDecimal getCeilingPrice() { return ceilingPrice; }
    public BigDecimal getSuggestedOffer() { return suggestedOffer; }
    public String getCreateTime() { return createTime; }
}
