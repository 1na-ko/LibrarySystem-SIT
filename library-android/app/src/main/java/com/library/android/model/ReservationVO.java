package com.library.android.model;

import com.google.gson.annotations.SerializedName;

/**
 * 预约信息.
 *
 * <p>WP-4 契约对齐：补 notifyTime 字段（后端 ReservationVO 含此字段，预约通知后展示用）.
 */
public class ReservationVO {

    @SerializedName("id")
    private long id;

    @SerializedName("book")
    private BookSimpleVO book;

    @SerializedName("reserveTime")
    private String reserveTime;

    /** WP-4：预约被通知的时间（队首读者收到归还通知的时刻）. */
    @SerializedName("notifyTime")
    private String notifyTime;

    @SerializedName("queuePosition")
    private int queuePosition;

    @SerializedName("status")
    private String status;

    @SerializedName("expireTime")
    private String expireTime;

    public long getId() { return id; }
    public BookSimpleVO getBook() { return book; }
    public String getReserveTime() { return reserveTime; }
    public String getNotifyTime() { return notifyTime; }
    public int getQueuePosition() { return queuePosition; }
    public String getStatus() { return status; }
    public String getExpireTime() { return expireTime; }

    public boolean isWaiting() { return "WAITING".equals(status); }
    public boolean isNotified() { return "NOTIFIED".equals(status); }
    public boolean canCancel() { return "WAITING".equals(status) || "NOTIFIED".equals(status); }
}