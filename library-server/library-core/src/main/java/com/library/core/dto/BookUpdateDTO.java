package com.library.core.dto;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 图书更新请求 DTO.
 * <p>
 * 对应 OpenAPI {@code BookUpdateRequest} Schema，用于 {@code PUT /admin/books/{id}}。
 * 所有字段均为可选，仅更新传入了非 null 值的字段。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookUpdateDTO {

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

    /** 总册数（≥ 1） */
    @Min(value = 1, message = "总册数至少为 1")
    private Integer totalCopies;

    /** 内容简介 */
    private String description;

    /** 馆藏位置 */
    private String location;

    /** 关键词（逗号分隔） */
    private String keywords;
}
