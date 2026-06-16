package com.library.android.model;

import com.google.gson.annotations.SerializedName;

/**
 * 知识图谱边（关系）.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
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
    public void setSourceId(long sourceId) { this.sourceId = sourceId; }

    public long getTargetId() { return targetId; }
    public void setTargetId(long targetId) { this.targetId = targetId; }

    public String getRelation() { return relation; }
    public void setRelation(String relation) { this.relation = relation; }

    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }
}
