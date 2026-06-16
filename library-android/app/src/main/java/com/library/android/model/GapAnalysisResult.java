package com.library.android.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * 馆藏缺口分析结果.
 */
public class GapAnalysisResult {

    @SerializedName("subjectId")
    private long subjectId;

    @SerializedName("subjectName")
    private String subjectName;

    @SerializedName("totalCoreBooks")
    private int totalCoreBooks;

    @SerializedName("ownedBooks")
    private int ownedBooks;

    @SerializedName("coverage")
    private double coverage;

    @SerializedName("gapBooks")
    private List<GapBook> gapBooks;

    public long getSubjectId() { return subjectId; }
    public String getSubjectName() { return subjectName; }
    public int getTotalCoreBooks() { return totalCoreBooks; }
    public int getOwnedBooks() { return ownedBooks; }
    public double getCoverage() { return coverage; }
    public List<GapBook> getGapBooks() { return gapBooks; }

    public static class GapBook {
        @SerializedName("isbn")
        private String isbn;

        @SerializedName("title")
        private String title;

        @SerializedName("author")
        private String author;

        @SerializedName("priority")
        private String priority;

        public String getIsbn() { return isbn; }
        public String getTitle() { return title; }
        public String getAuthor() { return author; }
        public String getPriority() { return priority; }
    }
}