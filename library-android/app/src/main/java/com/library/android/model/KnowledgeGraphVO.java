package com.library.android.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * 知识图谱 VO（节点 + 边）.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public class KnowledgeGraphVO {

    @SerializedName("nodes")
    private List<GraphNode> nodes;

    @SerializedName("edges")
    private List<GraphEdge> edges;

    public List<GraphNode> getNodes() { return nodes; }
    public void setNodes(List<GraphNode> nodes) { this.nodes = nodes; }

    public List<GraphEdge> getEdges() { return edges; }
    public void setEdges(List<GraphEdge> edges) { this.edges = edges; }
}
