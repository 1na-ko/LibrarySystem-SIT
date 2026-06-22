package com.library.core.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.core.entity.BorrowRecord;
import com.library.core.enums.BorrowStatusEnum;
import com.library.core.mapper.BorrowRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 超期借阅定时检查 Job.
 * <p>
 * 每天凌晨 3:00 扫描所有到期未还的借阅记录，自动标记为 OVERDUE 并生成罚款记录。
 * <p>
 * 采用分批扫描：每批 {@link #BATCH_SIZE} 条委托 {@link OverdueBatchProcessor} 在独立事务中
 * （{@code REQUIRES_NEW}）提交，处理后状态变为 OVERDUE 自动排除，避免大事务长时间持锁。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OverdueCheckJob {

    private final BorrowRecordMapper borrowRecordMapper;
    private final OverdueBatchProcessor batchProcessor;

    /** 单批扫描上限，防止数据量增长后一次性全量加载 */
    static final int BATCH_SIZE = 500;

    /**
     * 每天凌晨 3:00 检查超期借阅.
     * <p>
     * 扫描条件：due_date < 今天 AND status IN (BORROWED, RENEWED).
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void checkOverdue() {
        LocalDate today = LocalDate.now();
        log.info("超期检查开始: date={}", today);

        int processedCount = 0;
        // 游标 ID 推进：即使本批 processBatch 全部失败（状态未变），lastId 已前进，
        // 下一轮严格扫描更大 ID 的记录，避免同批被无限重复扫描。
        long lastId = 0L;
        // 防卡死兜底：限制最大迭代次数
        final int maxIterations = 1000;
        int iteration = 0;
        while (iteration++ < maxIterations) {
            final long cursor = lastId;
            List<BorrowRecord> batch = borrowRecordMapper.selectList(
                    new LambdaQueryWrapper<BorrowRecord>()
                            .gt(BorrowRecord::getId, cursor)
                            .lt(BorrowRecord::getDueDate, today)
                            .in(BorrowRecord::getStatus, BorrowStatusEnum.BORROWED, BorrowStatusEnum.RENEWED)
                            .orderByAsc(BorrowRecord::getId)
                            .last("LIMIT " + BATCH_SIZE)
            );
            if (batch.isEmpty()) {
                break;
            }
            processedCount += batchProcessor.processBatch(batch, today);
            // 推进游标到本批最后一条记录的 ID，确保下一轮严格扫描更大 ID 的记录
            lastId = batch.get(batch.size() - 1).getId();
        }
        if (iteration > maxIterations) {
            log.warn("超期检查达到最大迭代次数 {}，可能存在持续失败批次，请排查", maxIterations);
        }

        log.info("超期检查结束: 处理 {} 条超期记录", processedCount);
    }
}
