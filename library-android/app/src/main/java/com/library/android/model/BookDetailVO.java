package com.library.android.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * 图书详情（包含 BookVO 所有字段 + 额外信息）.
 */
public class BookDetailVO {

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

    // ---- 额外字段 ----
    @SerializedName("keywords")
    private List<String> keywords;

    @SerializedName("relatedBooks")
    private List<BookVO> relatedBooks;

    @SerializedName("reservationCount")
    private int reservationCount;

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
    public List<String> getKeywords() { return keywords; }
    public List<BookVO> getRelatedBooks() { return relatedBooks; }
    public int getReservationCount() { return reservationCount; }
}