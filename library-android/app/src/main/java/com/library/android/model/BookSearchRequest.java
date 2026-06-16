package com.library.android.model;

import com.google.gson.annotations.SerializedName;

/**
 * 图书搜索请求参数.
 */
public class BookSearchRequest {

    @SerializedName("keyword")
    private String keyword;

    @SerializedName("author")
    private String author;

    @SerializedName("isbn")
    private String isbn;

    @SerializedName("categoryId")
    private Long categoryId;

    @SerializedName("sortBy")
    private String sortBy = "relevance";

    @SerializedName("pageNum")
    private int pageNum = 1;

    @SerializedName("pageSize")
    private int pageSize = 20;

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
    public int getPageNum() { return pageNum; }
    public void setPageNum(int pageNum) { this.pageNum = pageNum; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
}