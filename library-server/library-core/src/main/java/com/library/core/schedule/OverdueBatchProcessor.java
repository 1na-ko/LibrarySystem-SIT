package com.library.core.schedule;

import com.library.core.entity.BorrowRecord;
import com.library.core.entity.FineRecord;
import com.library.core.enums.BorrowStatusEnum;
import com.library.core.mapper.BorrowRecordMapper;
import com.library.core.mapper.FineRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 超期记录批量处理器.
 * <p>
 * 从 {@link OverdueCheckJob} 中提取，确保每批在独立事务
 * （{@code REQUIRES_NEW}）中提交，避免 Spring AOP 自调用绕过事务代理。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OverdueBatchProcessor {

    private final BorrowRecordMapper borrowRecordMapper;
    private final FineRecordMapper fineRecordMapper;

    private static final BigDecimal DAILY_FINE = new BigDecimal("0.50");

    /**
     * 在独立事务中处理单批超期记录.
     *
     * @param batch 本批待处理的超期借阅记录
     * @param today 当前日期
     * @return 实际处理的记录数
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int processBatch(List<BorrowRecord> batch, LocalDate today) {
        int count = 0;
        for (BorrowRecord record : batch) {
            long overdueDays = ChronoUnit.DAYS.between(record.getDueDate(), today);
            if (overdueDays <= 0) {
                continue;
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
            count++;
        }
        return count;
    }
}
