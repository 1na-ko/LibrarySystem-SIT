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

    /** WP-0/4：无核心书目数据等友好提示（非空时前端展示，区分"暂无缺口"与"暂无数据"）. */
    @SerializedName("message")
    private String message;

    public long getSubjectId() { return subjectId; }
    public String getSubjectName() { return subjectName; }
    public int getTotalCoreBooks() { return totalCoreBooks; }
    public int getOwnedBooks() { return ownedBooks; }
    public double getCoverage() { return coverage; }
    public List<GapBook> getGapBooks() { return gapBooks; }
    public String getMessage() { return message; }

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