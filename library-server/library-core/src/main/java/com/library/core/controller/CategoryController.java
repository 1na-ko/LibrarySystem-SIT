package com.library.core.controller;

import com.library.common.result.Result;
import com.library.core.service.CategoryService;
import com.library.core.vo.CategoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 图书分类控制器.
 * <p>
 * 提供分类树、平铺列表和详情三个端点，所有认证用户均可访问。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 获取完整分类树（多级嵌套，含 children）.
     * <p>
     * 一次性返回所有分类节点，前端可直接渲染为树形组件。
     */
    @GetMapping("/tree")
    public Result<List<CategoryVO>> getTree() {
        return Result.success(categoryService.getTree());
    }

    /**
     * 按父级获取分类列表（平铺，不含 children）.
     * <p>
     * 不传 parentId 时返回顶级分类列表。
     *
     * @param parentId 父分类 ID（可选）
     */
    @GetMapping
    public Result<List<CategoryVO>> listByParentId(@RequestParam(required = false) Long parentId) {
        return Result.success(categoryService.listByParentId(parentId));
    }

    /**
     * 获取分类详情（含直接子分类列表）.
     *
     * @param id 分类 ID
     */
    @GetMapping("/{id}")
    public Result<CategoryVO> getById(@PathVariable Long id) {
        return Result.success(categoryService.getById(id));
    }
}
