package com.library.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.core.entity.BorrowRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 借阅记录 Mapper.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Mapper
public interface BorrowRecordMapper extends BaseMapper<BorrowRecord> {

    /**
     * 查询全量活跃借阅记录用于协同过滤矩阵构建.
     * <p>
     * 仅返回 {@code user_id} 和 {@code book_id}（其余字段为 null），
     * 用于内存中构建用户-图书交互矩阵。高校图书馆借阅量级（万级）内全量加载可行。
     *
     * @return 借阅记录列表（仅含 userId 和 bookId）
     */
    List<BorrowRecord> selectAllActiveForCF();

    /**
     * 批量查询用户在借数量（BORROWED/RENEWED）.
     *
     * @param userIds 用户 ID 列表
     * @return [{user_id → cnt}, ...]
     */
    List<Map<String, Object>> countCurrentBorrowsByUserIds(@Param("userIds") List<Long> userIds);

    /**
     * 批量查询用户超期数量（OVERDUE）.
     *
     * @param userIds 用户 ID 列表
     * @return [{user_id → cnt}, ...]
     */
    List<Map<String, Object>> countOverdueByUserIds(@Param("userIds") List<Long> userIds);

    /** Dashboard: 按借阅日期统计数量 */
    long countByDateRange(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /** Dashboard: 按归还日期统计数量 */
    long countByReturnDateRange(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /** Dashboard: 当前超期未还总数 */
    long countOverdue();

    /** Dashboard: 当前活跃借阅人数（去重） */
    long countDistinctActiveBorrowers();

    /** Dashboard: 按借阅日期分组统计 */
    List<Map<String, Object>> countByDateRangeGrouped(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /** Dashboard: 按归还日期分组统计 */
    List<Map<String, Object>> countByReturnDateRangeGrouped(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /** Dashboard: 热门分类 Top-N */
    List<Map<String, Object>> topBorrowCategories(@Param("limit") int limit);
}
