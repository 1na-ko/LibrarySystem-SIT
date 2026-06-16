package com.library.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.core.entity.Category;
import org.apache.ibatis.annotations.Mapper;

/**
 * 图书分类 Mapper.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}
