package com.library.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.core.entity.Reservation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 预约记录 Mapper.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Mapper
public interface ReservationMapper extends BaseMapper<Reservation> {
}
