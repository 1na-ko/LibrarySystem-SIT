package com.library.android.model;

import com.google.gson.annotations.SerializedName;

/**
 * 续借结果 VO.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
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
    public void setBorrowId(long borrowId) { this.borrowId = borrowId; }

    public String getOldDueDate() { return oldDueDate; }
    public void setOldDueDate(String oldDueDate) { this.oldDueDate = oldDueDate; }

    public String getNewDueDate() { return newDueDate; }
    public void setNewDueDate(String newDueDate) { this.newDueDate = newDueDate; }

    public int getRenewCount() { return renewCount; }
    public void setRenewCount(int renewCount) { this.renewCount = renewCount; }
}
