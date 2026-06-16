package com.library.core.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图书推荐视图对象.
 * <p>
 * 对应 OpenAPI {@code BookRecommendVO} Schema，用于相关图书和个性化推荐响应。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookRecommendVO {

    /** 推荐图书 */
    private BookSimpleVO book;

    /** 推荐分数（0-1） */
    private Double score;

    /** 推荐理由 */
    private String reason;
}
