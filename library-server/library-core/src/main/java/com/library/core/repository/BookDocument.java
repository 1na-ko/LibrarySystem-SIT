package com.library.core.repository;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)
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

    /** 分类 ID（用于按分类精确筛选，与 categoryName 冗余存储以支持 term 查询） */
    private Long categoryId;

    /** 分类名称 */
    private String categoryName;

    /** 累计借阅次数 */
    private Integer borrowCount;

    /** 可借册数 */
    private Integer availCopies;

    /** 封面 URL */
    private String coverUrl;

    /** 馆藏位置 */
    private String location;

    /**
     * 出版日期.
     * <p>
     * 必须显式声明 {@code @JsonFormat}，原因：
     * ES Java Client 内部的 JacksonJsonpMapper 在某些场景下不一定使用 Spring 注入的 ObjectMapper
     * （取决于 ES 客户端版本与 JsonpMapper 的 builder 配置）。
     * 不加注解时默认 ObjectMapper 无法把 ES 返回的 "2019-12-01" 字符串反序列化为 LocalDate，
     * 抛 "Failed to decode response" 导致搜索全部返回空。
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate pubDate;

    /** Completion Suggester 输入（书名 + 作者 + 关键词拆分） */
    private List<String> suggest;
}
