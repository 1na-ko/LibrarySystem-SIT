package com.library.android.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room Entity — 缓存的借阅记录.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Entity(tableName = "cached_borrows")
public class CachedBorrowEntity {

    @PrimaryKey
    public long id;

    @ColumnInfo(name = "user_id")
    public long userId;

    @ColumnInfo(name = "book_id")
    public long bookId;

    @ColumnInfo(name = "book_title")
    public String bookTitle;

    @ColumnInfo(name = "status")
    public String status;

    @ColumnInfo(name = "borrow_date")
    public String borrowDate;

    @ColumnInfo(name = "due_date")
    public String dueDate;

    @ColumnInfo(name = "json_payload")
    public String jsonPayload;

    @ColumnInfo(name = "cached_at")
    public long cachedAt;

    public CachedBorrowEntity() {}
}
