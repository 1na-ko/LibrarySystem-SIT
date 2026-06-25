package com.library.android.model;

import com.google.gson.annotations.SerializedName;

/**
 * 图书推荐.
 */
public class BookRecommendVO {

    @SerializedName("book")
    private BookSimpleVO book;

    @SerializedName("score")
    private double score;

    @SerializedName("reason")
    private String reason;

    public BookSimpleVO getBook() { return book; }
    public double getScore() { return score; }
    public String getReason() { return reason; }
}