package com.library.core.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 搜索自动补全建议视图.
 * <p>
 * 对应 OpenAPI {@code GET /books/suggest} 响应中 {@code data} 数组的元素 Schema。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuggestVO {

    /** 补全文本 */
    private String text;

    /** 建议类型：book / author / keyword */
    private String type;
}
