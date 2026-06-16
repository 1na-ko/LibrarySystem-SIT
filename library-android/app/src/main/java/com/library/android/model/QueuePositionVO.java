package com.library.android.model;

import com.google.gson.annotations.SerializedName;

/**
 * 排队位置 VO（预约排队查询响应）.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public class QueuePositionVO {

    @SerializedName("position")
    private int position;

    @SerializedName("totalWaiting")
    private int totalWaiting;

    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }

    public int getTotalWaiting() { return totalWaiting; }
    public void setTotalWaiting(int totalWaiting) { this.totalWaiting = totalWaiting; }
}
