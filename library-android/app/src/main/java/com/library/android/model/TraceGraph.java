package com.library.android.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * 文献溯源图 VO.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public class TraceGraph {

    @SerializedName("sourceBook")
    private BookVO sourceBook;

    @SerializedName("paths")
    private List<TracePath> paths;

    public BookVO getSourceBook() { return sourceBook; }
    public void setSourceBook(BookVO sourceBook) { this.sourceBook = sourceBook; }

    public List<TracePath> getPaths() { return paths; }
    public void setPaths(List<TracePath> paths) { this.paths = paths; }

    /** 溯源路径. */
    public static class TracePath {

        @SerializedName("nodes")
        private List<GraphNode> nodes;

        @SerializedName("edges")
        private List<GraphEdge> edges;

        @SerializedName("depth")
        private int depth;

        @SerializedName("totalWeight")
        private double totalWeight;

        public List<GraphNode> getNodes() { return nodes; }
        public void setNodes(List<GraphNode> nodes) { this.nodes = nodes; }

        public List<GraphEdge> getEdges() { return edges; }
        public void setEdges(List<GraphEdge> edges) { this.edges = edges; }

        public int getDepth() { return depth; }
        public void setDepth(int depth) { this.depth = depth; }

        public double getTotalWeight() { return totalWeight; }
        public void setTotalWeight(double totalWeight) { this.totalWeight = totalWeight; }
    }
}
