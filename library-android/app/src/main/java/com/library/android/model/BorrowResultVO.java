package com.library.android.model;

import com.google.gson.annotations.SerializedName;

/**
 * 借阅结果 VO.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
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
    public void setBorrowId(long borrowId) { this.borrowId = borrowId; }

    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }

    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
