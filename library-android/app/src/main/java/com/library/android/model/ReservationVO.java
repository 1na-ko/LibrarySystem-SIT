package com.library.android.model;

import com.google.gson.annotations.SerializedName;

/**
 * 预约信息.
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
    public BookSimpleVO getBook() { return book; }
    public String getReserveTime() { return reserveTime; }
    public int getQueuePosition() { return queuePosition; }
    public String getStatus() { return status; }
    public String getExpireTime() { return expireTime; }

    public boolean isWaiting() { return "WAITING".equals(status); }
    public boolean isNotified() { return "NOTIFIED".equals(status); }
    public boolean canCancel() { return "WAITING".equals(status) || "NOTIFIED".equals(status); }
}