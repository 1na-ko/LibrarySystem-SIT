package com.library.android.model;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

/**
 * 知识图谱节点.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
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
    public void setId(long id) { this.id = id; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Map<String, Object> getProperties() { return properties; }
    public void setProperties(Map<String, Object> properties) { this.properties = properties; }
}
