package com.library.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.core.entity.BorrowRecord;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

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
}
