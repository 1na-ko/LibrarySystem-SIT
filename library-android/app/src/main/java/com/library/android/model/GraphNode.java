package com.library.android.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

/**
 * 知识图谱节点.
 */
public class GraphNode {

    @SerializedName("id")
    private long id;

    @SerializedName("label")
    private String label;

    @SerializedName("type")
    private String type;

    @SerializedName("properties")
    private Map<String, Object> properties;

    public long getId() { return id; }
    public String getLabel() { return label; }
    public String getType() { return type; }
    public Map<String, Object> getProperties() { return properties; }
}