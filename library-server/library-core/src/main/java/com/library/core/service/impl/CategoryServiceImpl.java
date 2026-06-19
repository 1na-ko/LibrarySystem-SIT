package com.library.core.service.impl;

import com.library.common.exception.BizException;
import com.library.common.exception.ErrorCode;
import com.library.core.entity.Category;
import com.library.core.mapper.CategoryMapper;
import com.library.core.service.CategoryService;
import com.library.core.vo.CategoryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 分类服务实现.
 * <p>
 * 树形组装采用一次加载全部 + Java 内存构建策略（O(n)），避免 N+1 递归查询。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;

    @Override
    public List<CategoryVO> getTree() {
        List<Category> categories = categoryMapper.selectList(null);
        if (categories.isEmpty()) {
            return List.of();
        }

        // Entity → VO
        List<CategoryVO> vos = categories.stream()
                .map(this::toVO)
                .sorted(Comparator.comparingInt(CategoryVO::getSortOrder))
                .toList();

        // 按 parentId 分组
        Map<Long, List<CategoryVO>> parentMap = vos.stream()
                .filter(v -> v.getParentId() != null)
                .collect(Collectors.groupingBy(CategoryVO::getParentId));

        // 组装树：顶级节点收集 children，其余跳过
        List<CategoryVO> roots = new ArrayList<>();
        for (CategoryVO vo : vos) {
            List<CategoryVO> children = parentMap.get(vo.getId());
            if (children != null) {
                children.sort(Comparator.comparingInt(CategoryVO::getSortOrder));
                vo.setChildren(children);
            }
            if (vo.getParentId() == null) {
                roots.add(vo);
            }
        }

        log.debug("分类树组装完成，顶级节点数: {}", roots.size());
        return roots;
    }

    @Override
    public List<CategoryVO> listByParentId(Long parentId) {
        List<Category> categories = categoryMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Category>()
                        .eq(parentId != null, Category::getParentId, parentId)
                        .isNull(parentId == null, Category::getParentId)
                        .orderByAsc(Category::getSortOrder)
        );
        return categories.stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public CategoryVO getById(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BizException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        CategoryVO vo = toVO(category);

        // 加载子分类
        List<CategoryVO> children = listByParentId(id);
        vo.setChildren(children);

        return vo;
    }

    /**
     * Entity → VO 转换.
     */
    private CategoryVO toVO(Category entity) {
        return CategoryVO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .parentId(entity.getParentId())
                .sortOrder(entity.getSortOrder())
                .build();
    }

    @Override
    public List<Long> collectDescendantIds(Long parentId) {
        if (parentId == null) {
            return List.of();
        }
        // 一次加载全表（分类规模通常 < 1000，O(n) 内存构建优于递归 SQL）
        List<Category> all = categoryMapper.selectList(null);
        if (all.isEmpty()) {
            return List.of(parentId);
        }
        // parentId → children IDs
        Map<Long, List<Long>> childrenMap = new java.util.HashMap<>();
        for (Category c : all) {
            if (c.getParentId() != null) {
                childrenMap.computeIfAbsent(c.getParentId(), k -> new ArrayList<>()).add(c.getId());
            }
        }
        // BFS 收集
        List<Long> result = new ArrayList<>();
        result.add(parentId);
        java.util.Deque<Long> queue = new java.util.ArrayDeque<>();
        queue.add(parentId);
        java.util.Set<Long> visited = new java.util.HashSet<>();
        visited.add(parentId);
        while (!queue.isEmpty()) {
            Long cur = queue.poll();
            List<Long> kids = childrenMap.get(cur);
            if (kids != null) {
                for (Long kid : kids) {
                    if (visited.add(kid)) {
                        result.add(kid);
                        queue.add(kid);
                    }
                }
            }
        }
        return result;
    }
}
