package com.library.android.model;

import com.google.gson.annotations.SerializedName;

/**
 * 预约请求.
 */
public class ReservationRequest {

    @SerializedName("bookId")
    private long bookId;

    public ReservationRequest() {}

    public ReservationRequest(long bookId) {
        this.bookId = bookId;
    }

    public long getBookId() { return bookId; }
    public void setBookId(long bookId) { this.bookId = bookId; }
}