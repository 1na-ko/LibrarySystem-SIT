package com.library.core.service;

import com.library.core.vo.CategoryVO;

import java.util.List;

/**
 * 图书分类服务接口.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public interface CategoryService {

    /**
     * 获取完整分类树（多级嵌套）.
     *
     * @return 顶级分类列表，各含 children 子分类
     */
    List<CategoryVO> getTree();

    /**
     * 按父分类 ID 获取子分类平铺列表.
     *
     * @param parentId 父分类 ID，为 null 时返回顶级分类
     * @return 子分类列表（不含 children）
     */
    List<CategoryVO> listByParentId(Long parentId);

    /**
     * 根据 ID 获取分类详情.
     *
     * @param id 分类 ID
     * @return 分类 VO，含 children 子列表
     * @throws com.library.common.exception.BizException 分类不存在时抛出 CATEGORY_NOT_FOUND
     */
    CategoryVO getById(Long id);
}
