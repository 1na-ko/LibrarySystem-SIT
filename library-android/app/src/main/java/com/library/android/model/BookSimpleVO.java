package com.library.android.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

/**
 * 图书摘要信息（列表/嵌套引用等轻量场景）.
 *
 * <p>WP2.7：实现 Parcelable 替代已弃用的 Serializable，Bundle IPC 性能提升 ~6x.
 */
public class BookSimpleVO implements Parcelable {
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

    @SerializedName("categoryName")
    private String categoryName;

    @SerializedName("coverUrl")
    private String coverUrl;

    @SerializedName("availCopies")
    private int availCopies;

    public BookSimpleVO() {}

    protected BookSimpleVO(Parcel in) {
        id = in.readLong();
        isbn = in.readString();
        title = in.readString();
        author = in.readString();
        publisher = in.readString();
        categoryName = in.readString();
        coverUrl = in.readString();
        availCopies = in.readInt();
    }

    public static final Creator<BookSimpleVO> CREATOR = new Creator<BookSimpleVO>() {
        @Override
        public BookSimpleVO createFromParcel(Parcel in) {
            return new BookSimpleVO(in);
        }
        @Override
        public BookSimpleVO[] newArray(int size) {
            return new BookSimpleVO[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(isbn);
        dest.writeString(title);
        dest.writeString(author);
        dest.writeString(publisher);
        dest.writeString(categoryName);
        dest.writeString(coverUrl);
        dest.writeInt(availCopies);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public long getId() { return id; }
    public String getIsbn() { return isbn; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getPublisher() { return publisher; }
    public String getCategoryName() { return categoryName; }
    public String getCoverUrl() { return coverUrl; }
    public int getAvailCopies() { return availCopies; }

    /**
     * 由 {@link BookDetailVO} 摘录关键字段构造摘要视图.
     *
     * <p>用于详情页操作（借阅/预约确认弹窗）需要 BookSimpleVO 而后端返回 Detail 的场景.
     */
    public static BookSimpleVO fromDetail(BookDetailVO detail) {
        BookSimpleVO simple = new BookSimpleVO();
        if (detail == null) return simple;
        simple.id = detail.getId();
        simple.isbn = detail.getIsbn();
        simple.title = detail.getTitle();
        simple.author = detail.getAuthor();
        simple.publisher = detail.getPublisher();
        simple.categoryName = detail.getCategoryName();
        simple.coverUrl = detail.getCoverUrl();
        simple.availCopies = detail.getAvailCopies();
        return simple;
    }
}
