package com.library.android.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * 知识图谱（节点 + 边）.
 */
public class KnowledgeGraphVO {

    @SerializedName("nodes")
    private List<GraphNode> nodes;

    @SerializedName("edges")
    private List<GraphEdge> edges;

    public List<GraphNode> getNodes() { return nodes; }
    public List<GraphEdge> getEdges() { return edges; }
}