package com.library.android.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * 查重结果.
 */
public class DuplicateCheckResult {

    @SerializedName("isDuplicate")
    private boolean isDuplicate;

    @SerializedName("duplicates")
    private List<DuplicateItem> duplicates;

    public boolean isDuplicate() { return isDuplicate; }
    public List<DuplicateItem> getDuplicates() { return duplicates; }

    public static class DuplicateItem {
        @SerializedName("book")
        private BookVO book;

        @SerializedName("score")
        private double score;

        @SerializedName("matchStrategy")
        private String matchStrategy;

        public BookVO getBook() { return book; }
        public double getScore() { return score; }
        public String getMatchStrategy() { return matchStrategy; }
    }
}