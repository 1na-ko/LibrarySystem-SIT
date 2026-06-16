package com.library.android.model;

import com.google.gson.annotations.SerializedName;

/**
 * 预约记录 VO.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public class ReservationVO {

    @SerializedName("id")
    private long id;

    @SerializedName("book")
    private BookSimpleVO book;

    @SerializedName("reserveTime")
    private String reserveTime;

    @SerializedName("queuePosition")
    private int queuePosition;

    @SerializedName("status")
    private String status;

    @SerializedName("expireTime")
    private String expireTime;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public BookSimpleVO getBook() { return book; }
    public void setBook(BookSimpleVO book) { this.book = book; }

    public String getReserveTime() { return reserveTime; }
    public void setReserveTime(String reserveTime) { this.reserveTime = reserveTime; }

    public int getQueuePosition() { return queuePosition; }
    public void setQueuePosition(int queuePosition) { this.queuePosition = queuePosition; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getExpireTime() { return expireTime; }
    public void setExpireTime(String expireTime) { this.expireTime = expireTime; }

    // 便捷状态判断
    public boolean isWaiting() { return "WAITING".equals(status); }
    public boolean isNotified() { return "NOTIFIED".equals(status); }
    public boolean isReserved() { return "RESERVED".equals(status); }
    public boolean isCancelled() { return "CANCELLED".equals(status); }
    public boolean isCompleted() { return "COMPLETED".equals(status); }
    public boolean isExpired() { return "EXPIRED".equals(status); }
    public boolean canCancel() { return isWaiting() || isNotified(); }
}
