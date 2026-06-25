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
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

    @Test
    @DisplayName("批处理全部失败时游标应推进，避免同批反复扫描")
    void shouldAdvanceCursorEvenWhenBatchProcessingFails() {
        // 第一批 id=1-5，第二批 id=6-10，第三批返回空终止循环
        List<BorrowRecord> firstBatch = buildBatch(1L, 5L);
        List<BorrowRecord> secondBatch = buildBatch(6L, 10L);

        when(borrowRecordMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(firstBatch)
                .thenReturn(secondBatch)
                .thenReturn(List.of());
        // 模拟批处理全部失败（状态未变，返回 0）
        when(batchProcessor.processBatch(anyList(), any(LocalDate.class))).thenReturn(0);

        overdueCheckJob.checkOverdue();

        // 验证：selectList 仅被调用 3 次（而非 maxIterations=1000 次兜底），
        // 说明游标推进生效，避免了同批被无限重复扫描。
        verify(borrowRecordMapper, times(3)).selectList(any(LambdaQueryWrapper.class));
        verify(batchProcessor, times(2)).processBatch(anyList(), any(LocalDate.class));
    }

    private List<BorrowRecord> buildBatch(long startIdInclusive, long endIdInclusive) {
        List<BorrowRecord> batch = new ArrayList<>();
        for (long id = startIdInclusive; id <= endIdInclusive; id++) {
            BorrowRecord r = new BorrowRecord();
            r.setId(id);
            r.setUserId(1L);
            r.setBookId(10L);
            r.setDueDate(LocalDate.now().minusDays(5));
            r.setStatus(BorrowStatusEnum.BORROWED);
            batch.add(r);
        }
        return batch;
    }
}
