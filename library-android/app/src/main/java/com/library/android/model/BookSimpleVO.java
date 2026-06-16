package com.library.android.model;

import com.google.gson.annotations.SerializedName;

/**
 * 图书摘要 VO（用于列表、嵌套引用等轻量场景）.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
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
    public void setId(long id) { this.id = id; }

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public int getAvailCopies() { return availCopies; }
    public void setAvailCopies(int availCopies) { this.availCopies = availCopies; }
}
