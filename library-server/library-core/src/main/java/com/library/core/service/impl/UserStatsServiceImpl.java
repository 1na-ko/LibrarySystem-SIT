package com.library.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.core.entity.Book;
import com.library.core.entity.BorrowRecord;
import com.library.core.entity.Category;
import com.library.core.enums.BorrowStatusEnum;
import com.library.core.mapper.BookMapper;
import com.library.core.mapper.BorrowRecordMapper;
import com.library.core.mapper.CategoryMapper;
import com.library.core.service.UserStatsService;
import com.library.core.vo.UserStatsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户借阅统计服务实现.
 * <p>
 * 聚合当前用户的借阅统计数据：总量、在借数、超期数、罚款总额、
 * 分类分布（饼图数据）和近 12 月月度趋势。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserStatsServiceImpl implements UserStatsService {

    private final BorrowRecordMapper borrowRecordMapper;
    private final BookMapper bookMapper;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional(readOnly = true)
    public UserStatsVO getStats(Long userId) {
        // 全部借阅记录
        List<BorrowRecord> allRecords = borrowRecordMapper.selectList(
                new LambdaQueryWrapper<BorrowRecord>()
                        .eq(BorrowRecord::getUserId, userId)
        );

        // 基本统计
        long totalBorrows = allRecords.size();
        long currentBorrows = allRecords.stream()
                .filter(r -> r.getStatus() == BorrowStatusEnum.BORROWED
                        || r.getStatus() == BorrowStatusEnum.RENEWED)
                .count();
        long totalOverdue = allRecords.stream()
                .filter(r -> r.getStatus() == BorrowStatusEnum.OVERDUE)
                .count();

        // 罚款总额
        BigDecimal totalFines = allRecords.stream()
                .map(r -> r.getFineAmount() != null ? r.getFineAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 分类分布
        List<UserStatsVO.CategoryStat> categoryDistribution = buildCategoryDistribution(allRecords);

        // 近 12 月趋势
        List<UserStatsVO.MonthlyStat> monthlyTrend = buildMonthlyTrend(allRecords);

        return UserStatsVO.builder()
                .totalBorrows(totalBorrows)
                .currentBorrows(currentBorrows)
                .totalOverdue(totalOverdue)
                .totalFines(totalFines)
                .categoryDistribution(categoryDistribution)
                .monthlyTrend(monthlyTrend)
                .build();
    }

    /**
     * 构建分类借阅分布.
     */
    private List<UserStatsVO.CategoryStat> buildCategoryDistribution(List<BorrowRecord> records) {
        if (records.isEmpty()) {
            return List.of();
        }

        List<Long> bookIds = records.stream()
                .map(BorrowRecord::getBookId)
                .distinct()
                .toList();
        List<Book> books = bookMapper.selectBatchIds(bookIds);
        Map<Long, Long> bookIdToCategoryId = books.stream()
                .collect(Collectors.toMap(Book::getId,
                        b -> b.getCategoryId() != null ? b.getCategoryId() : 0L));

        List<Category> categories = categoryMapper.selectList(null);
        Map<Long, String> catIdToName = categories.stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));

        Map<String, Long> categoryCountMap = new LinkedHashMap<>();
        for (BorrowRecord record : records) {
            Long catId = bookIdToCategoryId.getOrDefault(record.getBookId(), 0L);
            String catName = catIdToName.getOrDefault(catId, "未分类");
            categoryCountMap.merge(catName, 1L, Long::sum);
        }

        return categoryCountMap.entrySet().stream()
                .map(e -> UserStatsVO.CategoryStat.builder()
                        .categoryName(e.getKey())
                        .count(e.getValue())
                        .build())
                .sorted((a, b) -> Long.compare(b.getCount(), a.getCount()))
                .toList();
    }

    /**
     * 构建近 12 月月度借阅趋势.
     */
    private List<UserStatsVO.MonthlyStat> buildMonthlyTrend(List<BorrowRecord> records) {
        LocalDate now = LocalDate.now();
        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("yyyy-MM");
        List<UserStatsVO.MonthlyStat> trend = new ArrayList<>();

        for (int i = 11; i >= 0; i--) {
            LocalDate monthStart = now.minusMonths(i).withDayOfMonth(1);
            String monthKey = monthStart.format(monthFmt);
            long count = records.stream()
                    .filter(r -> {
                        LocalDate refDate = r.getBorrowDate() != null ? r.getBorrowDate()
                                : r.getCreateTime().toLocalDate();
                        return refDate.getYear() == monthStart.getYear()
                                && refDate.getMonthValue() == monthStart.getMonthValue();
                    })
                    .count();
            trend.add(UserStatsVO.MonthlyStat.builder()
                    .month(monthKey)
                    .count(count)
                    .build());
        }

        return trend;
    }
}
