package com.library.android.model;

import com.google.gson.annotations.SerializedName;

/**
 * 借书操作结果.
 */
public class BorrowResultVO {

    @SerializedName("borrowId")
    private long borrowId;

    @SerializedName("bookTitle")
    private String bookTitle;

    @SerializedName("dueDate")
    private String dueDate;

    @SerializedName("status")
    private String status;

    public long getBorrowId() { return borrowId; }
    public String getBookTitle() { return bookTitle; }
    public String getDueDate() { return dueDate; }
    public String getStatus() { return status; }
    public boolean isBorrowed() { return "BORROWED".equals(status); }
}