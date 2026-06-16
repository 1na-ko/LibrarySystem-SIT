package com.library.android.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * 图书详情 VO（含关键词、相关图书、预约人数）.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public class BookDetailVO extends BookVO {

    @SerializedName("keywords")
    private List<String> keywords;

    @SerializedName("relatedBooks")
    private List<BookVO> relatedBooks;

    @SerializedName("reservationCount")
    private int reservationCount;

    public List<String> getKeywords() { return keywords; }
    public void setKeywords(List<String> keywords) { this.keywords = keywords; }

    public List<BookVO> getRelatedBooks() { return relatedBooks; }
    public void setRelatedBooks(List<BookVO> relatedBooks) { this.relatedBooks = relatedBooks; }

    public int getReservationCount() { return reservationCount; }
    public void setReservationCount(int reservationCount) { this.reservationCount = reservationCount; }
}
