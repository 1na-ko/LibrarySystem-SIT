package com.library.android.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * 分页结果包装类，对应后端 {@code PageResult<T>} 结构.
 *
 * @param <T> 记录类型
 * @author LibrarySystem Team
 * @since 1.0.0
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

    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPageNum() {
        return pageNum;
    }

    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    /** 是否还有下一页. */
    public boolean hasNextPage() {
        return pageNum < totalPages;
    }
}
