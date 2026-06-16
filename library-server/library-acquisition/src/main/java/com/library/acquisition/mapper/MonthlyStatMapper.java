package com.library.acquisition.mapper;

import com.library.acquisition.dto.MonthlyStatDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 借阅按月+按学科聚合查询 Mapper.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Mapper
public interface MonthlyStatMapper {

    /**
     * 按学科聚合借阅月度统计.
     */
    @Select("SELECT DATE_FORMAT(br.borrow_date, '%Y-%m') AS ym, "
            + "b.category_id AS subjectId, COUNT(*) AS borrowCount "
            + "FROM borrow_record br JOIN book b ON br.book_id = b.id AND b.deleted = 0 "
            + "WHERE br.deleted = 0 AND br.borrow_date BETWEEN #{start} AND #{end} "
            + "AND br.status IN ('BORROWED', 'RENEWED', 'RETURNED', 'OVERDUE') "
            + "GROUP BY DATE_FORMAT(br.borrow_date, '%Y-%m'), b.category_id ORDER BY ym LIMIT 10000")
    List<MonthlyStatDTO> aggregateByMonthAndCategory(@Param("start") LocalDate start,
                                                      @Param("end") LocalDate end);
}
