package com.library.core.vo;

import com.library.core.entity.Book;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 图书精简视图对象.
 * <p>
 * 用于借阅列表、预约列表、搜索结果列表等非详情的展示场景，
 * 仅含核心识別字段，减少数据传输量。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookSimpleVO {

    /** 图书 ID */
    private Long id;

    /** ISBN 号 */
    private String isbn;

    /** 书名 */
    private String title;

    /** 作者 */
    private String author;

    /** 出版社 */
    private String publisher;

    /** 封面 URL */
    private String coverUrl;

    /** 出版日期 */
    private LocalDate pubDate;

    /** 可借册数 */
    private Integer availCopies;

    /** 分类名称（用于搜索/列表展示） */
    private String categoryName;

    /**
     * 从 Book 实体构建 BookSimpleVO（统一各处 Book→VO 转换，消除重复代码）.
     *
     * @param book         图书实体
     * @param categoryName 分类名称（可为 null）
     * @return BookSimpleVO
     */
    public static BookSimpleVO from(Book book, String categoryName) {
        return BookSimpleVO.builder()
                .id(book.getId())
                .isbn(book.getIsbn())
                .title(book.getTitle())
                .author(book.getAuthor())
                .publisher(book.getPublisher())
                .coverUrl(book.getCoverUrl())
                .pubDate(book.getPubDate())
                .availCopies(book.getAvailCopies())
                .categoryName(categoryName)
                .build();
    }
}
