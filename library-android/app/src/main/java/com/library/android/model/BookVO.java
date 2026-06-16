package com.library.android.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * 图书详情 VO（含馆藏位置、册数等完整信息）.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public class BookVO implements Serializable {

    private static final long serialVersionUID = 1L;

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
    public void setId(long id) { this.id = id; }

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }

    public String getPubDate() { return pubDate; }
    public void setPubDate(String pubDate) { this.pubDate = pubDate; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public int getAvailCopies() { return availCopies; }
    public void setAvailCopies(int availCopies) { this.availCopies = availCopies; }

    public int getTotalCopies() { return totalCopies; }
    public void setTotalCopies(int totalCopies) { this.totalCopies = totalCopies; }

    public int getBorrowCount() { return borrowCount; }
    public void setBorrowCount(int borrowCount) { this.borrowCount = borrowCount; }
}
