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

        List<BorrowRecord> overdueList = borrowRecordMapper.selectList(
                new LambdaQueryWrapper<BorrowRecord>()
                        .lt(BorrowRecord::getDueDate, today)
                        .in(BorrowRecord::getStatus, BorrowStatusEnum.BORROWED, BorrowStatusEnum.RENEWED)
        );

        if (overdueList.isEmpty()) {
            log.info("超期检查结束: 无超期记录");
            return;
        }

        int processedCount = 0;
        for (BorrowRecord record : overdueList) {
            // 计算超期天数
            long overdueDays = ChronoUnit.DAYS.between(record.getDueDate(), today);
            if (overdueDays <= 0) {
                continue;
            }

            // 更新状态为 OVERDUE
            record.setStatus(BorrowStatusEnum.OVERDUE);
            borrowRecordMapper.updateById(record);

            // 生成罚款记录
            BigDecimal fineAmount = DAILY_FINE.multiply(BigDecimal.valueOf(overdueDays));
            FineRecord fineRecord = new FineRecord();
            fineRecord.setBorrowId(record.getId());
            fineRecord.setAmount(fineAmount);
            fineRecord.setReason("超期 " + overdueDays + " 天，日罚款 0.5 元");
            fineRecord.setPaid(0);
            fineRecordMapper.insert(fineRecord);

            processedCount++;
            log.info("超期处理: borrowId={}, userId={}, overdueDays={}, fine={}",
                    record.getId(), record.getUserId(), overdueDays, fineAmount);
        }

        log.info("超期检查结束: 处理 {} 条超期记录", processedCount);
    }
}
