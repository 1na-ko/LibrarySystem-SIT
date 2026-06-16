package com.library.android.model;

import com.google.gson.annotations.SerializedName;

/**
 * 图书完整信息.
 */
public class BookVO {

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

    @SerializedName("pubDate")
    private String pubDate;

    @SerializedName("categoryName")
    private String categoryName;

    @SerializedName("description")
    private String description;

    @SerializedName("coverUrl")
    private String coverUrl;

    @SerializedName("location")
    private String location;

    @SerializedName("availCopies")
    private int availCopies;

    @SerializedName("totalCopies")
    private int totalCopies;

    @SerializedName("borrowCount")
    private int borrowCount;

    public long getId() { return id; }
    public String getIsbn() { return isbn; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getPublisher() { return publisher; }
    public String getPubDate() { return pubDate; }
    public String getCategoryName() { return categoryName; }
    public String getDescription() { return description; }
    public String getCoverUrl() { return coverUrl; }
    public String getLocation() { return location; }
    public int getAvailCopies() { return availCopies; }
    public int getTotalCopies() { return totalCopies; }
    public int getBorrowCount() { return borrowCount; }
}