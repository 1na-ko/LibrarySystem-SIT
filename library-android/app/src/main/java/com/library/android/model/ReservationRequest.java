package com.library.android.model;

import com.google.gson.annotations.SerializedName;

/**
 * 预约请求体.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public class ReservationRequest {

    @SerializedName("bookId")
    private long bookId;

    public ReservationRequest(long bookId) {
        this.bookId = bookId;
    }

    public long getBookId() { return bookId; }
}
