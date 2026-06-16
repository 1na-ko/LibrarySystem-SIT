package com.library.core.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 图书详情视图对象.
 * <p>
 * 用于图书详情页展示，含全量字段。{@code reservationCount} 在 Phase 4 预约模块实现前暂为 0。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookDetailVO {

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

    /** 出版日期 */
    private LocalDate pubDate;

    /** 分类 ID */
    private Long categoryId;

    /** 分类名称 */
    private String categoryName;

    /** 总册数 */
    private Integer totalCopies;

    /** 可借册数 */
    private Integer availCopies;

    /** 内容简介 */
    private String description;

    /** 封面 URL */
    private String coverUrl;

    /** 馆藏位置 */
    private String location;

    /** 关键词（逗号分隔） */
    private String keywords;

    /** 累计借阅次数 */
    private Integer borrowCount;

    /** 当前预约人数（Phase 4 实现，暂为 0） */
    @Builder.Default
    private Integer reservationCount = 0;
}
