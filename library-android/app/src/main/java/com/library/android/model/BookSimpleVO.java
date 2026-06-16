package com.library.android.model;

import com.google.gson.annotations.SerializedName;

/**
 * 图书摘要信息（列表/嵌套引用等轻量场景）.
 */
public class BookSimpleVO {

    @SerializedName("id")
    private long id;

    @SerializedName("isbn")
    private String isbn;

    @SerializedName("title")
    private String title;

    @SerializedName("author")
    private String author;

    @SerializedName("publisher")
    private String publisher;

    @SerializedName("categoryName")
    private String categoryName;

    @SerializedName("coverUrl")
    private String coverUrl;

    @SerializedName("availCopies")
    private int availCopies;

    public long getId() { return id; }
    public String getIsbn() { return isbn; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getPublisher() { return publisher; }
    public String getCategoryName() { return categoryName; }
    public String getCoverUrl() { return coverUrl; }
    public int getAvailCopies() { return availCopies; }
}