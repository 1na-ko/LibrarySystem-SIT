package com.library.android.model;

import com.google.gson.annotations.SerializedName;

/**
 * 图书推荐 VO（含推荐分数与理由）.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public class BookRecommendVO {

    @SerializedName("book")
    private BookSimpleVO book;

    @SerializedName("score")
    private double score;

    @SerializedName("reason")
    private String reason;

    public BookSimpleVO getBook() { return book; }
    public void setBook(BookSimpleVO book) { this.book = book; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
