package com.library.android.model;

import com.google.gson.annotations.SerializedName;

/**
 * 借书请求.
 */
public class BorrowRequest {

    @SerializedName("bookId")
    private long bookId;

    public BorrowRequest() {}

    public BorrowRequest(long bookId) {
        this.bookId = bookId;
    }

    public long getBookId() { return bookId; }
    public void setBookId(long bookId) { this.bookId = bookId; }
}