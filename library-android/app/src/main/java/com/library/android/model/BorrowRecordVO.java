package com.library.android.model;

import com.google.gson.annotations.SerializedName;

/**
 * 借阅记录.
 */
public class BorrowRecordVO {

    @SerializedName("id")
    private long id;

    @SerializedName("book")
    private BookSimpleVO book;

    @SerializedName("borrowDate")
    private String borrowDate;

    @SerializedName("dueDate")
    private String dueDate;

    @SerializedName("returnDate")
    private String returnDate;

    @SerializedName("renewCount")
    private int renewCount;

    @SerializedName("status")
    private String status;

    @SerializedName("fineAmount")
    private Double fineAmount;

    public long getId() { return id; }
    public BookSimpleVO getBook() { return book; }
    public String getBorrowDate() { return borrowDate; }
    public String getDueDate() { return dueDate; }
    public String getReturnDate() { return returnDate; }
    public int getRenewCount() { return renewCount; }
    public String getStatus() { return status; }
    public Double getFineAmount() { return fineAmount; }

    public boolean isOverdue() { return "OVERDUE".equals(status); }
    public boolean isReturned() { return "RETURNED".equals(status); }
    public boolean isBorrowing() { return "BORROWED".equals(status) || "RENEWED".equals(status); }
}