package com.library.android.model;

import com.google.gson.annotations.SerializedName;

/**
 * 知识图谱边.
 */
public class GraphEdge {

    @SerializedName("sourceId")
    private long sourceId;

    @SerializedName("targetId")
    private long targetId;

    @SerializedName("relation")
    private String relation;

    @SerializedName("weight")
    private double weight;

    public long getSourceId() { return sourceId; }
    public long getTargetId() { return targetId; }
    public String getRelation() { return relation; }
    public double getWeight() { return weight; }
}