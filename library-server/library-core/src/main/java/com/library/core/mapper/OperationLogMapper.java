package com.library.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.core.entity.OperationLogEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作日志 Mapper.
 * <p>
 * 仅提供 {@code insert} 语义——操作日志不可编辑、不可删除。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLogEntity> {
}
