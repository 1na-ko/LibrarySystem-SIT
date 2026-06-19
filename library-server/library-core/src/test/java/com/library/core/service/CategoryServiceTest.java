package com.library.core.service;

import com.library.common.exception.BizException;
import com.library.core.entity.Category;
import com.library.core.mapper.CategoryMapper;
import com.library.core.service.impl.CategoryServiceImpl;
import com.library.core.vo.CategoryVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * CategoryService 单元测试.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@DisplayName("CategoryService")
@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category computerScience;
    private Category programming;
    private Category literature;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        computerScience = new Category();
        computerScience.setId(1L);
        computerScience.setName("计算机科学");
        computerScience.setParentId(null);
        computerScience.setSortOrder(1);
        computerScience.setDeleted(0);
        computerScience.setCreateTime(now);
        computerScience.setUpdateTime(now);

        programming = new Category();
        programming.setId(101L);
        programming.setName("编程语言");
        programming.setParentId(1L);
        programming.setSortOrder(1);
        programming.setDeleted(0);
        programming.setCreateTime(now);
        programming.setUpdateTime(now);

        literature = new Category();
        literature.setId(2L);
        literature.setName("文学");
        literature.setParentId(null);
        literature.setSortOrder(2);
        literature.setDeleted(0);
        literature.setCreateTime(now);
        literature.setUpdateTime(now);
    }

    @Nested
    @DisplayName("getTree")
    class GetTree {

        @Test
        @DisplayName("有数据时应返回多级嵌套分类树")
        void shouldReturnTreeWhenCategoriesExist() {
            when(categoryMapper.selectList(null))
                    .thenReturn(List.of(computerScience, programming, literature));

            List<CategoryVO> tree = categoryService.getTree();

            assertThat(tree).hasSize(2);
            CategoryVO cs = tree.get(0);
            assertThat(cs.getName()).isEqualTo("计算机科学");
            assertThat(cs.getChildren()).hasSize(1);
            assertThat(cs.getChildren().get(0).getName()).isEqualTo("编程语言");
        }

        @Test
        @DisplayName("无数据时应返回空列表")
        void shouldReturnEmptyTreeWhenNoCategories() {
            when(categoryMapper.selectList(null)).thenReturn(Collections.emptyList());

            List<CategoryVO> tree = categoryService.getTree();

            assertThat(tree).isEmpty();
        }
    }

    @Nested
    @DisplayName("listByParentId")
    class ListByParentId {

        @Test
        @DisplayName("传入 parentId 时应返回该父级下的子分类")
        void shouldReturnChildrenWhenParentIdGiven() {
            when(categoryMapper.selectList(org.mockito.ArgumentMatchers.any()))
                    .thenReturn(List.of(programming));

            List<CategoryVO> list = categoryService.listByParentId(1L);

            assertThat(list).hasSize(1);
            assertThat(list.get(0).getName()).isEqualTo("编程语言");
        }
    }

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("ID 存在时应返回分类详情含子分类")
        void shouldReturnCategoryWhenIdExists() {
            when(categoryMapper.selectById(1L)).thenReturn(computerScience);
            when(categoryMapper.selectList(org.mockito.ArgumentMatchers.any()))
                    .thenReturn(List.of(programming));

            CategoryVO vo = categoryService.getById(1L);

            assertThat(vo.getName()).isEqualTo("计算机科学");
            assertThat(vo.getChildren()).hasSize(1);
            assertThat(vo.getChildren().get(0).getName()).isEqualTo("编程语言");
        }

        @Test
        @DisplayName("ID 不存在时应抛出 CATEGORY_NOT_FOUND")
        void shouldThrowBizExceptionWhenCategoryNotFound() {
            when(categoryMapper.selectById(999L)).thenReturn(null);

            assertThatThrownBy(() -> categoryService.getById(999L))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("分类不存在");
        }
    }

    @Nested
    @DisplayName("collectDescendantIds (WP-0)")
    class CollectDescendantIds {

        @Test
        @DisplayName("顶级分类应返回自身+所有子孙")
        void shouldCollectAllDescendantsForRoot() {
            // 计算机科学(1) -> 编程语言(101) -> 编程语言深层(10101)
            Category deep = new Category();
            deep.setId(10101L);
            deep.setName("Java");
            deep.setParentId(101L);
            deep.setSortOrder(1);

            when(categoryMapper.selectList(null))
                    .thenReturn(List.of(computerScience, programming, literature, deep));

            List<Long> ids = categoryService.collectDescendantIds(1L);

            assertThat(ids).containsExactlyInAnyOrder(1L, 101L, 10101L);
        }

        @Test
        @DisplayName("叶子分类应只返回自身")
        void shouldReturnSelfWhenLeaf() {
            when(categoryMapper.selectList(null))
                    .thenReturn(List.of(computerScience, programming, literature));

            List<Long> ids = categoryService.collectDescendantIds(101L);

            assertThat(ids).containsExactly(101L);
        }

        @Test
        @DisplayName("不存在的分类应返回自身（容错）")
        void shouldReturnSelfWhenNotFound() {
            when(categoryMapper.selectList(null))
                    .thenReturn(List.of(computerScience));

            List<Long> ids = categoryService.collectDescendantIds(9999L);

            assertThat(ids).containsExactly(9999L);
        }

        @Test
        @DisplayName("null 输入应返回空列表")
        void shouldReturnEmptyWhenNull() {
            assertThat(categoryService.collectDescendantIds(null)).isEmpty();
        }

        @Test
        @DisplayName("分类表为空时应只返回自身")
        void shouldReturnSelfWhenTableEmpty() {
            when(categoryMapper.selectList(null)).thenReturn(Collections.emptyList());

            assertThat(categoryService.collectDescendantIds(1L)).containsExactly(1L);
        }
    }
}
