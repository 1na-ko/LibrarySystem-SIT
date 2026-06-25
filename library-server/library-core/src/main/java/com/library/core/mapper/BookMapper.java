package com.library.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.core.entity.Book;
import org.apache.ibatis.annotations.Mapper;

/**
 * 图书 Mapper.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Mapper
public interface BookMapper extends BaseMapper<Book> {
}
