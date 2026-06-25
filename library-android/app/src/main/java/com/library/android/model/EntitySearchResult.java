package com.library.android.model;

import com.google.gson.annotations.SerializedName;

/**
 * 知识实体搜索结果 VO.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public class EntitySearchResult {

    @SerializedName("entityId")
    private long entityId;

    @SerializedName("entityName")
    private String entityName;

    @SerializedName("entityType")
    private String entityType;

    @SerializedName("pagerank")
    private double pagerank;

    public long getEntityId() { return entityId; }
    public void setEntityId(long entityId) { this.entityId = entityId; }

    public String getEntityName() { return entityName; }
    public void setEntityName(String entityName) { this.entityName = entityName; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public double getPagerank() { return pagerank; }
    public void setPagerank(double pagerank) { this.pagerank = pagerank; }
}
