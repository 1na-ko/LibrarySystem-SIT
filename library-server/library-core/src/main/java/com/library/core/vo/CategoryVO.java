package com.library.core.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 分类视图对象（支持树形结构）.
 * <p>
 * {@code children} 字段用于组装多级分类树，默认初始化为空列表以避免前端 null 判断。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryVO {

    /** 分类 ID */
    private Long id;

    /** 分类名称 */
    private String name;

    /** 父分类 ID（NULL 表示顶级分类） */
    private Long parentId;

    /** 排序序号 */
    private Integer sortOrder;

    /** 子分类列表 */
    @Builder.Default
    private List<CategoryVO> children = new ArrayList<>();
}
