package com.library.core.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 图书创建请求 DTO.
 * <p>
 * 对应 OpenAPI {@code BookCreateRequest} Schema，用于 {@code POST /admin/books}。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookCreateDTO {

    /** ISBN 号（必填，唯一） */
    @NotBlank(message = "ISBN 不能为空")
    private String isbn;

    /** 书名（必填） */
    @NotBlank(message = "书名不能为空")
    private String title;

    /** 作者（必填） */
    @NotBlank(message = "作者不能为空")
    private String author;

    /** 出版社 */
    private String publisher;

    /** 出版日期 */
    private LocalDate pubDate;

    /** 分类 ID（必填） */
    @NotNull(message = "分类不能为空")
    private Long categoryId;

    /** 总册数（必填，至少 1 册） */
    @NotNull(message = "总册数不能为空")
    @Min(value = 1, message = "总册数至少为 1")
    private Integer totalCopies;

    /** 内容简介 */
    private String description;

    /** 馆藏位置 */
    private String location;

    /** 关键词（逗号分隔） */
    private String keywords;
}
