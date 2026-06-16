package com.library.android.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room Entity — 缓存的图书摘要.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Entity(tableName = "cached_books")
public class CachedBookEntity {

    @PrimaryKey
    public long id;

    @ColumnInfo(name = "isbn")
    public String isbn;

    @ColumnInfo(name = "title")
    public String title;

    @ColumnInfo(name = "author")
    public String author;

    @ColumnInfo(name = "publisher")
    public String publisher;

    @ColumnInfo(name = "category_name")
    public String categoryName;

    @ColumnInfo(name = "cover_url")
    public String coverUrl;

    @ColumnInfo(name = "avail_copies")
    public int availCopies;

    @ColumnInfo(name = "json_payload")
    public String jsonPayload;

    @ColumnInfo(name = "cached_at")
    public long cachedAt;

    public CachedBookEntity() {}
}
