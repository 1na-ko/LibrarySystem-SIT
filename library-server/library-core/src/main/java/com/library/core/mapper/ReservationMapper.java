package com.library.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.core.entity.Reservation;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 预约记录 Mapper.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Mapper
public interface ReservationMapper extends BaseMapper<Reservation> {

    /**
     * 物理删除逻辑删除的 WAITING 记录.
     * <p>V6 UNIQUE(user_id,book_id,status) 约束在 MySQL 层不感知 MyBatis-Plus deleted 标记，
     * 逻辑删除后残留 (uid,bid,WAITING) 仍占据索引槽位，阻塞用户重新预约。此方法在 reserve() 中前置调用，
     * 在 insert 前清理干净，消除 DataIntegrityViolationException。
     */
    @Delete("DELETE FROM reservation WHERE user_id = #{userId} AND book_id = #{bookId} AND status = 'WAITING' AND deleted = 1")
    int physicalCleanStaleWaiting(@Param("userId") Long userId, @Param("bookId") Long bookId);
}
