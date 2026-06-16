package com.library.acquisition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.acquisition.entity.NegotiationRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NegotiationMapper extends BaseMapper<NegotiationRecord> {
}
