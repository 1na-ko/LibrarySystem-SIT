package com.library.android.model;

import com.google.gson.annotations.SerializedName;

/**
 * 续借结果.
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
}