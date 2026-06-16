package com.library.core.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.core.entity.BorrowRecord;
import com.library.core.entity.FineRecord;
import com.library.core.enums.BorrowStatusEnum;
import com.library.core.mapper.BorrowRecordMapper;
import com.library.core.mapper.FineRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 超期借阅定时检查 Job.
 * <p>
 * 每天凌晨 3:00 扫描所有到期未还的借阅记录，自动标记为 OVERDUE 并生成罚款记录。
 * <p>
 * 采用分批扫描：每批处理 {@link #BATCH_SIZE} 条后状态即变为 OVERDUE，下次查询自动排除
 * 已处理记录，避免一次性加载全量数据导致内存压力（数据量增长后仍可控）。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OverdueCheckJob {

    private final BorrowRecordMapper borrowRecordMapper;
    private final FineRecordMapper fineRecordMapper;

    private static final BigDecimal DAILY_FINE = new BigDecimal("0.50");
    /** 单批扫描上限，防止数据量增长后一次性全量加载 */
    private static final int BATCH_SIZE = 500;

    /**
     * 每天凌晨 3:00 检查超期借阅.
     * <p>
     * 扫描条件：due_date < 今天 AND status IN (BORROWED, RENEWED).
     * 处理：status → OVERDUE，生成 FineRecord。
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void checkOverdue() {
        LocalDate today = LocalDate.now();
        log.info("超期检查开始: date={}", today);

        int processedCount = 0;
        // 分批扫描：每批处理 BATCH_SIZE 条后状态变为 OVERDUE，下次查询自动排除，
        // 故每次取「尚未处理的前 N 条」直至为空，避免一次性全量加载。
        while (true) {
            List<BorrowRecord> batch = borrowRecordMapper.selectList(
                    new LambdaQueryWrapper<BorrowRecord>()
                            .lt(BorrowRecord::getDueDate, today)
                            .in(BorrowRecord::getStatus, BorrowStatusEnum.BORROWED, BorrowStatusEnum.RENEWED)
                            .last("LIMIT " + BATCH_SIZE)
            );
            if (batch.isEmpty()) {
                break;
            }
            for (BorrowRecord record : batch) {
                if (processOverdue(record, today)) {
                    processedCount++;
                }
            }
        }

        log.info("超期检查结束: 处理 {} 条超期记录", processedCount);
    }

    /**
     * 处理单条超期记录：状态置 OVERDUE + 生成罚款.
     *
     * @param record 借阅记录
     * @param today  当前日期
     * @return 是否实际处理（overdueDays > 0 时为 true）
     */
    private boolean processOverdue(BorrowRecord record, LocalDate today) {
        long overdueDays = ChronoUnit.DAYS.between(record.getDueDate(), today);
        if (overdueDays <= 0) {
            return false;
        }

        record.setStatus(BorrowStatusEnum.OVERDUE);
        borrowRecordMapper.updateById(record);

        BigDecimal fineAmount = DAILY_FINE.multiply(BigDecimal.valueOf(overdueDays));
        FineRecord fineRecord = new FineRecord();
        fineRecord.setBorrowId(record.getId());
        fineRecord.setAmount(fineAmount);
        fineRecord.setReason("超期 " + overdueDays + " 天，日罚款 0.5 元");
        fineRecord.setPaid(0);
        fineRecordMapper.insert(fineRecord);

        log.info("超期处理: borrowId={}, userId={}, overdueDays={}, fine={}",
                record.getId(), record.getUserId(), overdueDays, fineAmount);
        return true;
    }
}
