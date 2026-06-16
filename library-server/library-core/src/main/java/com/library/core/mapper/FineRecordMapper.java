package com.library.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.core.entity.FineRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 罚款记录 Mapper.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Mapper
public interface FineRecordMapper extends BaseMapper<FineRecord> {
}
