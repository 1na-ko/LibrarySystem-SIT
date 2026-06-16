package com.library.core.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.core.entity.BorrowRecord;
import com.library.core.enums.BorrowStatusEnum;
import com.library.core.mapper.BorrowRecordMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
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
    private OverdueBatchProcessor batchProcessor;

    @InjectMocks
    private OverdueCheckJob overdueCheckJob;

    @Test
    @DisplayName("超期记录存在时应委托 batchProcessor 处理")
    void shouldDelegateToBatchProcessorWhenOverdueRecordsExist() {
        BorrowRecord overdue = new BorrowRecord();
        overdue.setId(1L);
        overdue.setUserId(1L);
        overdue.setBookId(10L);
        overdue.setDueDate(LocalDate.now().minusDays(5));
        overdue.setStatus(BorrowStatusEnum.BORROWED);

        // 分批扫描：首轮返回超期记录，次轮返回空以终止循环
        when(borrowRecordMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(overdue))
                .thenReturn(List.of());
        when(batchProcessor.processBatch(anyList(), any(LocalDate.class))).thenReturn(1);

        overdueCheckJob.checkOverdue();

        verify(batchProcessor).processBatch(anyList(), any(LocalDate.class));
    }

    @Test
    @DisplayName("无超期记录时应跳过不做处理")
    void shouldSkipWhenNoOverdueRecords() {
        when(borrowRecordMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of());

        overdueCheckJob.checkOverdue();

        verify(batchProcessor, never()).processBatch(anyList(), any(LocalDate.class));
    }
}
