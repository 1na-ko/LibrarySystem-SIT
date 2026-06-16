package com.library.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.core.entity.BorrowRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 借阅记录 Mapper.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Mapper
public interface BorrowRecordMapper extends BaseMapper<BorrowRecord> {
}
