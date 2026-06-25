package com.library.android.model;

import com.google.gson.annotations.SerializedName;

/**
 * 续借结果.
 *
 * <p>WP-4 契约对齐：后端 RenewResultVO 无 maxRenewReached 字段（前端原误增），
 * 续借次数上限改由 UI 端基于 renewCount 与产品规则（默认上限 1）判断.
 */
public class RenewResultVO {

    @SerializedName("borrowId")
    private long borrowId;

    @SerializedName("oldDueDate")
    private String oldDueDate;

    @SerializedName("newDueDate")
    private String newDueDate;

    @SerializedName("renewCount")
    private int renewCount;

    public long getBorrowId() { return borrowId; }
    public String getOldDueDate() { return oldDueDate; }
    public String getNewDueDate() { return newDueDate; }
    public int getRenewCount() { return renewCount; }

    /** 续借次数上限判断：默认续借上限 1，UI 据此决定续借按钮可用性. */
    public boolean isMaxRenewReached() {
        return renewCount >= 1;
    }
}
