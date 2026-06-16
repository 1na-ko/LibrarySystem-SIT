package com.library.core.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.core.entity.BorrowRecord;
import com.library.core.enums.BorrowStatusEnum;
import com.library.core.mapper.BorrowRecordMapper;
import com.library.core.mapper.FineRecordMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * OverdueCheckJob 单元测试.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@DisplayName("OverdueCheckJob")
@ExtendWith(MockitoExtension.class)
class OverdueCheckJobTest {

    @Mock
    private BorrowRecordMapper borrowRecordMapper;
    @Mock
    private FineRecordMapper fineRecordMapper;

    @InjectMocks
    private OverdueCheckJob overdueCheckJob;

    @Test
    @DisplayName("超期记录存在时应更新状态并生成罚款")
    void shouldUpdateStatusAndGenerateFineWhenOverdueRecordsExist() {
        BorrowRecord overdue = new BorrowRecord();
        overdue.setId(1L);
        overdue.setUserId(1L);
        overdue.setBookId(10L);
        overdue.setDueDate(LocalDate.now().minusDays(5));
        overdue.setStatus(BorrowStatusEnum.BORROWED);

        when(borrowRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(overdue));
        when(borrowRecordMapper.updateById(any(BorrowRecord.class))).thenReturn(1);
        when(fineRecordMapper.insert(any())).thenReturn(1);

        overdueCheckJob.checkOverdue();

        verify(borrowRecordMapper).updateById(any(BorrowRecord.class));
        verify(fineRecordMapper).insert(any());
    }

    @Test
    @DisplayName("无超期记录时应跳过不做处理")
    void shouldSkipWhenNoOverdueRecords() {
        when(borrowRecordMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of());

        overdueCheckJob.checkOverdue();

        verify(borrowRecordMapper, never()).updateById(any());
        verify(fineRecordMapper, never()).insert(any());
    }
}
