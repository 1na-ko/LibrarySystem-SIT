package com.library.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.core.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
