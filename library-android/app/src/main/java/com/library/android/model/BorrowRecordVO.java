package com.library.android.model;

import com.google.gson.annotations.SerializedName;

/**
 * 借阅记录 VO.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
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
    public void setId(long id) { this.id = id; }

    public BookSimpleVO getBook() { return book; }
    public void setBook(BookSimpleVO book) { this.book = book; }

    public String getBorrowDate() { return borrowDate; }
    public void setBorrowDate(String borrowDate) { this.borrowDate = borrowDate; }

    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }

    public String getReturnDate() { return returnDate; }
    public void setReturnDate(String returnDate) { this.returnDate = returnDate; }

    public int getRenewCount() { return renewCount; }
    public void setRenewCount(int renewCount) { this.renewCount = renewCount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getFineAmount() { return fineAmount; }
    public void setFineAmount(Double fineAmount) { this.fineAmount = fineAmount; }

    // 便捷状态判断方法
    public boolean isBorrowed() { return "BORROWED".equals(status); }
    public boolean isOverdue() { return "OVERDUE".equals(status); }
    public boolean isReturned() { return "RETURNED".equals(status); }
    public boolean isRenewed() { return "RENEWED".equals(status); }
    public boolean canRenew() { return (isBorrowed() || isRenewed()) && renewCount < 1; }
}
