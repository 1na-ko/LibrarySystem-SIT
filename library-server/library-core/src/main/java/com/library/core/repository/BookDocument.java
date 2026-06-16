package com.library.core.repository;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Elasticsearch 图书文档模型.
 * <p>
 * 对应 ES {@code books} 索引的文档结构，用于全文检索与自动补全。
 * 索引 mapping 定义见 {@code EsIndexInitializer}，严格对齐架构文档 §5.3。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookDocument {

    /** 图书 ID（与 MySQL 主键一致） */
    private Long id;

    /** ISBN 号 */
    private String isbn;

    /** 书名 */
    private String title;

    /** 作者 */
    private String author;

    /** 出版社 */
    private String publisher;

    /** 内容简介 */
    private String description;

    /** 关键词（逗号分隔） */
    private String keywords;

    /** 分类名称 */
    private String categoryName;

    /** 累计借阅次数 */
    private Integer borrowCount;

    /** 可借册数 */
    private Integer availCopies;

    /** 出版日期 */
    private LocalDate pubDate;

    /** Completion Suggester 输入（书名 + 作者 + 关键词拆分） */
    private List<String> suggest;
}
