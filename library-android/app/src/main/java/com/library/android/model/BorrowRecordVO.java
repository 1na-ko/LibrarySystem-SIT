package com.library.android.model;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

/**
 * 借阅记录.
 *
 * <p>B.5 字段对齐：fineAmount 由 Double 改 BigDecimal 与后端契约对齐
 * （后端 {@code BorrowRecordVO.fineAmount} 类型为 BigDecimal，避免浮点精度损失）.
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
    private BigDecimal fineAmount;

    public long getId() { return id; }
    public BookSimpleVO getBook() { return book; }
    public String getBorrowDate() { return borrowDate; }
    public String getDueDate() { return dueDate; }
    public String getReturnDate() { return returnDate; }
    public int getRenewCount() { return renewCount; }
    public String getStatus() { return status; }
    public BigDecimal getFineAmount() { return fineAmount; }

    /** UI 展示用便捷方法：避免 BigDecimal 直接 format 时为 null 触发 NPE. */
    public double getFineAmountDouble() {
        return fineAmount == null ? 0.0 : fineAmount.doubleValue();
    }

    public boolean isOverdue() { return "OVERDUE".equals(status); }
    public boolean isReturned() { return "RETURNED".equals(status); }
    public boolean isBorrowing() { return "BORROWED".equals(status) || "RENEWED".equals(status); }
    public boolean canRenew() { return "BORROWED".equals(status); }
}
