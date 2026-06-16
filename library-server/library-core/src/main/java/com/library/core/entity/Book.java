package com.library.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 图书实体（对应 book 表）.
 * <p>
 * version 字段用于乐观锁，由 MyBatis-Plus {@code OptimisticLockerInnerInterceptor} 自动管理。
 * 逻辑删除列 {@code deleted} 由全局配置 {@code logic-delete-field} 处理。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
@TableName("book")
public class Book {

    /** 主键 */
    @TableId(type = IdType.AUTO)
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

    /** 总册数 */
    private Integer totalCopies;

    /** 可借册数 */
    private Integer availCopies;

    /** 内容简介 */
    private String description;

    /** 封面 URL */
    private String coverUrl;

    /** 馆藏位置（如：A区-3架-12层） */
    private String location;

    /** 关键词（逗号分隔，用于搜索） */
    private String keywords;

    /** 累计借阅次数 */
    private Integer borrowCount;

    /** 乐观锁版本号 */
    private Integer version;

    /** 逻辑删除（0=未删除, 1=已删除） */
    private Integer deleted;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
