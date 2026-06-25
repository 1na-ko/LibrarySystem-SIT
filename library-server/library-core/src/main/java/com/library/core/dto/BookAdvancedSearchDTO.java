package com.library.core.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图书高级组合搜索请求 DTO.
 * <p>
 * 对应 {@code GET /books/search/advanced} 查询参数，所有字段均为可选。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookAdvancedSearchDTO {

    /** 书名（模糊匹配） */
    private String title;

    /** 作者（精确匹配） */
    private String author;

    /** ISBN（精确匹配） */
    private String isbn;

    /** 出版社（模糊匹配） */
    private String publisher;

    /** 出版年份起始 */
    private Integer pubYearFrom;

    /** 出版年份截止 */
    private Integer pubYearTo;

    /** 分类筛选 */
    private Long categoryId;

    /** 仅显示有库存的 */
    private Boolean onlyAvailable;

    /** 页码（1-based，默认 1） */
    @Min(value = 1, message = "页码最小为 1")
    @Builder.Default
    private Integer pageNum = 1;

    /** 每页条数（默认 20，最大 100） */
    @Min(value = 1, message = "每页至少 1 条")
    @Max(value = 100, message = "每页最多 100 条")
    @Builder.Default
    private Integer pageSize = 20;
}
