package com.library.android.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * 分页结果 — 对应后端 PageResult.
 */
public class PageResult<T> {

    @SerializedName("records")
    private List<T> records;

    @SerializedName("total")
    private long total;

    @SerializedName("pageNum")
    private int pageNum;

    @SerializedName("pageSize")
    private int pageSize;

    @SerializedName("totalPages")
    private int totalPages;

    public List<T> getRecords() { return records; }
    public long getTotal() { return total; }
    public int getPageNum() { return pageNum; }
    public int getPageSize() { return pageSize; }
    public int getTotalPages() { return totalPages; }
}