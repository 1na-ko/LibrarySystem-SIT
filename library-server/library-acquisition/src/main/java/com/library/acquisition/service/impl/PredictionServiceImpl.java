package com.library.acquisition.service.impl;

import com.library.acquisition.algorithm.SimplifiedArima;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.library.acquisition.config.AcquisitionProperties;
import com.library.acquisition.dto.MonthlyStatDTO;
import com.library.acquisition.mapper.MonthlyStatMapper;
import com.library.acquisition.service.PredictionService;
import com.library.acquisition.vo.PurchasePredictionVO;
import com.library.core.entity.Category;
import com.library.core.entity.Reservation;
import com.library.core.mapper.CategoryMapper;
import com.library.core.mapper.ReservationMapper;
import com.library.core.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class PredictionServiceImpl implements PredictionService {

    private final MonthlyStatMapper monthlyStatMapper;
    private final CategoryMapper categoryMapper;
    private final ReservationMapper reservationMapper;
    private final CategoryService categoryService;
    private final AcquisitionProperties props;
    private static final SimplifiedArima ARIMA = new SimplifiedArima();

    @Override
    public List<PurchasePredictionVO> predict(Long subjectId, int months) {
        Category category = categoryMapper.selectById(subjectId);
        String subjectName = category != null ? category.getName() : "学科#" + subjectId;

        // WP-0 修复：递归子分类——前端可选任意层级，顶级分类下书的 categoryId 是叶子分类
        Set<Long> descendantIds = new HashSet<>(categoryService.collectDescendantIds(subjectId));

        // 1. 取近N月借阅序列（按学科聚合）
        LocalDate end = LocalDate.now().withDayOfMonth(1);
        LocalDate start = end.minusMonths(props.getPredictionHistoryMonths());
        List<MonthlyStatDTO> stats = monthlyStatMapper.aggregateByMonthAndCategory(start, end);

        // 按 subjectId 及所有子孙分类过滤，按月聚合（同月多分类合并）
        java.util.Map<String, Double> monthSum = new java.util.TreeMap<>();
        for (MonthlyStatDTO s : stats) {
            if (s.getSubjectId() != null && descendantIds.contains(s.getSubjectId())) {
                monthSum.merge(s.getYm(), (double) s.getCount(), Double::sum);
            }
        }
        double[] history = monthSum.values().stream().mapToDouble(Double::doubleValue).toArray();

        // 2. ARIMA 预测（宽容模式：数据不足降级为简单移动平均，不再抛 503）
        double[] forecast = ARIMA.forecastLenient(history, months);
        boolean dataInsufficient = !ARIMA.hasSufficientData(history);

        // 3. 业务因子修正——预约热度同样递归子分类
        long reservationHeat = 0;
        if (!descendantIds.isEmpty()) {
            String idsCsv = descendantIds.stream().map(String::valueOf)
                    .collect(java.util.stream.Collectors.joining(","));
            reservationHeat = reservationMapper.selectCount(
                    new QueryWrapper<Reservation>()
                            .apply("book_id IN (SELECT id FROM book WHERE category_id IN (" + idsCsv + ") AND deleted = 0)")
                            .ge("reserve_time", LocalDate.now().minusDays(30)));
        }

        // 4. 生成结果
        List<PurchasePredictionVO> list = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");
        LocalDate cursor = LocalDate.now().withDayOfMonth(1).plusMonths(1);
        String message = dataInsufficient
                ? "历史数据不足（仅 " + history.length + " 个月），预测仅供参考"
                : null;
        for (int i = 0; i < months; i++) {
            // 数据不足时 confidence 直接钳制为低（最低 0.2）
            double base = 1.0 - props.getPredictionConfidenceDecay() * i
                    - (dataInsufficient ? props.getPredictionDataScarcityPenalty() : 0);
            double confidence = dataInsufficient
                    ? Math.max(0.2, Math.min(0.5, base))
                    : Math.max(0.3, Math.min(0.95, base));
            // 按每个预测目标月份独立计算季节因子
            double sFactor = getSeasonFactor(cursor.plusMonths(i));
            int adjusted = (int) Math.round(forecast[i] * sFactor
                    * (1 + reservationHeat * props.getPredictionReservationHeatCoefficient())
                    * props.getPredictionBaseMultiplier());

            list.add(PurchasePredictionVO.builder()
                    .subjectId(subjectId)
                    .subjectName(subjectName)
                    .month(cursor.plusMonths(i).format(fmt))
                    .predictedDemand(adjusted)
                    .confidence(confidence)
                    .features(PurchasePredictionVO.FeatureMap.builder()
                            .historyTrend(forecast[i])
                            .seasonFactor(sFactor)
                            .reservationHeat((int) reservationHeat)
                            .build())
                    .message(message)
                    .build());
        }
        return list;
    }

    /**
     * 按目标月份计算季节因子（开学季加权 / 假期降权）.
     *
     * @param targetMonth 目标月份（如 2026-07）
     */
    private double getSeasonFactor(LocalDate targetMonth) {
        int month = targetMonth.getMonthValue();
        // 开学季 2-3月、8-9月
        if (month == 2 || month == 3 || month == 8 || month == 9) return props.getSeasonExamFactor();
        // 寒暑假
        if (month == 1 || month == 7) return props.getSeasonVacationFactor();
        return 1.0;
    }
}
