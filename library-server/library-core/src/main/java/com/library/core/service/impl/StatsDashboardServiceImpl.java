package com.library.core.service.impl;

import com.library.core.enums.BorrowStatusEnum;
import com.library.core.mapper.BorrowRecordMapper;
import com.library.core.service.StatsDashboardService;
import com.library.core.vo.DashboardVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 流通统计 Dashboard Service 实现.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatsDashboardServiceImpl implements StatsDashboardService {

    private final BorrowRecordMapper borrowRecordMapper;

    @Override
    @Transactional(readOnly = true)
    public DashboardVO getDashboard() {
        LocalDate today = LocalDate.now();
        YearMonth yearMonth = YearMonth.now();
        LocalDate monthStart = yearMonth.atDay(1);

        // 今日借阅数
        long todayBorrows = borrowRecordMapper.countByDateRange(today, today);
        // 今日归还数
        long todayReturns = borrowRecordMapper.countByReturnDateRange(today, today);
        // 当前超期数
        long todayOverdue = borrowRecordMapper.countOverdue();
        // 实时在馆人数
        long activeBorrowers = borrowRecordMapper.countDistinctActiveBorrowers();

        // 本月日趋势
        List<Map<String, Object>> borrowTrend = borrowRecordMapper.countByDateRangeGrouped(monthStart, today);
        List<Map<String, Object>> returnTrend = borrowRecordMapper.countByReturnDateRangeGrouped(monthStart, today);

        Map<String, Long> borrowMap = borrowTrend.stream()
                .collect(Collectors.toMap(m -> m.get("date").toString(),
                        m -> ((Number) m.get("cnt")).longValue(), (a, b) -> a));
        Map<String, Long> returnMap = returnTrend.stream()
                .collect(Collectors.toMap(m -> m.get("date").toString(),
                        m -> ((Number) m.get("cnt")).longValue(), (a, b) -> a));

        List<DashboardVO.DailyTrend> monthTrend = new ArrayList<>();
        for (LocalDate d = monthStart; !d.isAfter(today); d = d.plusDays(1)) {
            String dateStr = d.toString();
            monthTrend.add(DashboardVO.DailyTrend.builder()
                    .date(dateStr)
                    .borrows(borrowMap.getOrDefault(dateStr, 0L))
                    .returns(returnMap.getOrDefault(dateStr, 0L))
                    .build());
        }

        // 热门分类 Top-10
        List<Map<String, Object>> topCategories = borrowRecordMapper.topBorrowCategories(10);
        List<DashboardVO.CategoryHotStat> hotCategories = topCategories.stream()
                .map(m -> DashboardVO.CategoryHotStat.builder()
                        .categoryName((String) m.get("category_name"))
                        .borrowCount(((Number) m.get("cnt")).longValue())
                        .build())
                .collect(Collectors.toList());

        return DashboardVO.builder()
                .todayBorrows(todayBorrows)
                .todayReturns(todayReturns)
                .todayOverdue(todayOverdue)
                .activeBorrowers(activeBorrowers)
                .monthTrend(monthTrend)
                .hotCategories(hotCategories)
                .build();
    }
}
