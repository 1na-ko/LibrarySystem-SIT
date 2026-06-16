package com.library.android.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * 文献溯源轨迹图.
 */
public class TraceGraph {

    @SerializedName("sourceBook")
    private BookVO sourceBook;

    @SerializedName("paths")
    private List<Path> paths;

    public BookVO getSourceBook() { return sourceBook; }
    public List<Path> getPaths() { return paths; }

    public static class Path {
        @SerializedName("nodes")
        private List<GraphNode> nodes;

        @SerializedName("edges")
        private List<GraphEdge> edges;

        @SerializedName("depth")
        private int depth;

        @SerializedName("totalWeight")
        private double totalWeight;

        public List<GraphNode> getNodes() { return nodes; }
        public List<GraphEdge> getEdges() { return edges; }
        public int getDepth() { return depth; }
        public double getTotalWeight() { return totalWeight; }
    }
}