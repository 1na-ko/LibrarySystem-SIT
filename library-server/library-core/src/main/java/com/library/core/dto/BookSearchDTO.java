package com.library.core.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图书关键词搜索请求 DTO.
 * <p>
 * 对应 {@code GET /books/search} 查询参数，含 Jakarta Validation 注解。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookSearchDTO {

    /** 搜索关键词（必填） */
    @NotBlank(message = "搜索关键词不能为空")
    private String keyword;

    /** 作者筛选（可选） */
    private String author;

    /** 分类筛选（可选） */
    private Long categoryId;

    /** 排序方式：relevance / borrowCount / pubDate（默认 relevance） */
    private String sortBy;

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
