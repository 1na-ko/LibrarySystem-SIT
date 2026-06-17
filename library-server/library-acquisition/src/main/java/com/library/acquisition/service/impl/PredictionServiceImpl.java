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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PredictionServiceImpl implements PredictionService {

    private final MonthlyStatMapper monthlyStatMapper;
    private final CategoryMapper categoryMapper;
    private final ReservationMapper reservationMapper;
    private final AcquisitionProperties props;
    private static final SimplifiedArima ARIMA = new SimplifiedArima();

    @Override
    public List<PurchasePredictionVO> predict(Long subjectId, int months) {
        Category category = categoryMapper.selectById(subjectId);
        String subjectName = category != null ? category.getName() : "学科#" + subjectId;

        // 1. 取近N月借阅序列（按学科聚合）
        LocalDate end = LocalDate.now().withDayOfMonth(1);
        LocalDate start = end.minusMonths(props.getPredictionHistoryMonths());
        List<MonthlyStatDTO> stats = monthlyStatMapper.aggregateByMonthAndCategory(start, end);

        // 按 subjectId 过滤
        List<MonthlyStatDTO> subjectStats = stats.stream()
                .filter(s -> subjectId.equals(s.getSubjectId()))
                .collect(Collectors.toList());

        double[] history = subjectStats.stream()
                .mapToDouble(MonthlyStatDTO::getCount)
                .toArray();

        // 2. ARIMA 预测
        double[] forecast = ARIMA.forecast(history, months);

        // 3. 业务因子修正（季节因子按每个预测月份独立计算，见循环内 getSeasonFactor(cursor.plusMonths(i))）
        long reservationHeat = reservationMapper.selectCount(
                new QueryWrapper<Reservation>()
                        .apply("book_id IN (SELECT id FROM book WHERE category_id = {0} AND deleted = 0)", subjectId)
                        .ge("reserve_time", LocalDate.now().minusDays(30)));

        // 4. 生成结果
        List<PurchasePredictionVO> list = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");
        LocalDate cursor = LocalDate.now().withDayOfMonth(1).plusMonths(1);
        for (int i = 0; i < months; i++) {
            double confidence = Math.max(0.3, Math.min(0.95,
                    1.0 - props.getPredictionConfidenceDecay() * i
                    - (history.length < props.getPredictionMinMonths() ? props.getPredictionDataScarcityPenalty() : 0)));
            // 按每个预测目标月份独立计算季节因子（而非所有月份共用当前月）
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
