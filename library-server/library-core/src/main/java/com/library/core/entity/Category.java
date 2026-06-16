package com.library.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 图书分类实体（对应 category 表）.
 * <p>
 * category 表为自引用树形结构，parentId 指向父分类。
 * 逻辑删除列 {@code deleted} 由全局配置 {@code logic-delete-field} 处理，无需 {@code @TableLogic}。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
@TableName("category")
public class Category {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分类名称 */
    private String name;

    /** 父分类 ID（NULL 表示顶级分类） */
    private Long parentId;

    /** 排序序号 */
    private Integer sortOrder;

    /** 逻辑删除（0=未删除, 1=已删除） */
    private Integer deleted;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
