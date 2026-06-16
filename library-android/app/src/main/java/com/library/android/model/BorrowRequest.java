package com.library.android.model;

import com.google.gson.annotations.SerializedName;

/**
 * 借书请求体.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public class BorrowRequest {

    @SerializedName("bookId")
    private long bookId;

    public BorrowRequest(long bookId) {
        this.bookId = bookId;
    }

    public long getBookId() { return bookId; }
}
