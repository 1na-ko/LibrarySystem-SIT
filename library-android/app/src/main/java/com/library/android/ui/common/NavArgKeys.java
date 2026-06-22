package com.library.android.ui.common;

/**
 * 导航参数/Bundle key 统一常量，消除魔法字符串.
 *
 * <p>使用方式：{@code args.putLong(NavArgKeys.BOOK_ID, bookId)} /
 * {@code getArguments().getLong(NavArgKeys.BOOK_ID, 0)}.
 *
 * @author LibrarySystem Team
 * @since 1.1.0
 */
public final class NavArgKeys {

    private NavArgKeys() { /* 工具类不可实例化 */ }

    /** 图书 ID（Fragment: BookDetail/KG/Trace; Activity: BookEdit）. */
    public static final String BOOK_ID = "bookId";

    /** 借阅记录 ID（BorrowDetailFragment）. */
    public static final String BORROW_ID = "borrowId";

    /** 谈判记录 ID（NegotiationDetailFragment）. */
    public static final String NEGOTIATION_ID = "negotiationId";

    /** 条码扫描 ISBN 结果. */
    public static final String ISBN = "isbn";

    /** 扫码来源标识（"borrow" / "search"）. */
    public static final String SOURCE = "source";

    /** 图书 Parcelable（BorrowConfirmDialog）. */
    public static final String BOOK = "book";

    /** 分类 ID（SearchFragment 分类导航）. */
    public static final String CATEGORY_ID = "categoryId";

    /** 分类名称（SearchFragment 分类导航）. */
    public static final String CATEGORY_NAME = "categoryName";

    /** 高级搜索：书名. */
    public static final String TITLE = "title";

    /** 高级搜索：作者. */
    public static final String AUTHOR = "author";

    /** 高级搜索：出版社. */
    public static final String PUBLISHER = "publisher";

    /** 高级搜索：关键词. */
    public static final String KEYWORD = "keyword";
}
