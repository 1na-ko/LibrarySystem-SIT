package com.library.android.model;

/**
 * 分页请求参数.
 */
public class PageRequest {

    private int pageNum = 1;
    private int pageSize = 20;

    public PageRequest() {}

    public PageRequest(int pageNum, int pageSize) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }

    public int getPageNum() { return pageNum; }
    public void setPageNum(int pageNum) { this.pageNum = pageNum; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
}