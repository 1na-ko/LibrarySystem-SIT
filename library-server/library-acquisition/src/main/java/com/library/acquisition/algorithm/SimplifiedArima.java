package com.library.acquisition.algorithm;

import com.library.common.exception.BizException;
import com.library.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;

import java.util.Arrays;

/**
 * 简化 ARIMA(1,1,1) 预测算法.
 * <p>
 * 基于 Apache Commons Math OLSMultipleLinearRegression 实现：
 * I 组件（差分）→ AR 组件（自回归）→ MA 组件（移动平均残差修正）→ 递推预测 → 逆差分还原.
 * 算法骨架参照架构文档 §8.2.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
public class SimplifiedArima {

    private static final int MIN_DATA_POINTS = 6;

    /**
     * 预测未来值（严格模式：数据不足抛异常，向后兼容）.
     *
     * @param history 历史时间序列（至少 6 个数据点）
     * @param steps   预测步数
     * @return 预测值数组（长度 = steps）
     * @throws BizException PREDICTION_DATA_INSUFFICIENT 数据点不足
     */
    public double[] forecast(double[] history, int steps) {
        if (history == null || history.length < MIN_DATA_POINTS) {
            throw new BizException(ErrorCode.PREDICTION_DATA_INSUFFICIENT,
                    "历史数据点 " + (history != null ? history.length : 0) + " < " + MIN_DATA_POINTS);
        }
        return forecastInternal(history, steps);
    }

    /**
     * 预测未来值（宽容模式：数据不足时降级为简单移动平均，不抛异常）.
     *
     * <p>WP-0：原 forecast 在数据不足时抛 PREDICTION_DATA_INSUFFICIENT → HTTP 503，
     * 用户看到"服务不可用"误以为系统故障；实际是该分类历史数据稀疏。
     * 改为降级返回，调用方据 history.length 与返回值合理标注 confidence/message。
     *
     * @param history 历史时间序列（可能为空或不足 MIN_DATA_POINTS）
     * @param steps   预测步数
     * @return 预测值数组（长度 = steps）；history 为空时返回全零；不足时返回平均值常数序列
     */
    public double[] forecastLenient(double[] history, int steps) {
        if (steps <= 0) return new double[0];
        if (history == null || history.length == 0) {
            return new double[steps]; // 全零
        }
        if (history.length < MIN_DATA_POINTS) {
            // 简单移动平均降级：取历史平均值作为预测常数
            double sum = 0;
            for (double v : history) sum += v;
            double avg = Math.max(0, sum / history.length);
            double[] result = new double[steps];
            Arrays.fill(result, avg);
            return result;
        }
        return forecastInternal(history, steps);
    }

    /** 历史数据是否足以做正常 ARIMA 预测. */
    public boolean hasSufficientData(double[] history) {
        return history != null && history.length >= MIN_DATA_POINTS;
    }

    private double[] forecastInternal(double[] history, int steps) {
        if (steps <= 0) return new double[0];

        // Step 1: 一阶差分（d=1）
        double[] diffed = difference(history);

        // Step 2: 构建 AR(1) 回归矩阵 + OLS 拟合
        int n = diffed.length;
        int sampleSize = n - 1; // AR(1) 滞后 1 期
        OLSMultipleLinearRegression ols = new OLSMultipleLinearRegression();
        double[] y = new double[sampleSize];
        double[][] x = new double[sampleSize][2]; // [lag, intercept]

        for (int t = 1; t < n; t++) {
            y[t - 1] = diffed[t];
            x[t - 1][0] = diffed[t - 1]; // AR(1) 滞后项
            x[t - 1][1] = 1.0;           // 常数项（截距）
        }

        ols.newSampleData(y, x);
        double[] coefficients;
        try {
            coefficients = ols.estimateRegressionParameters(); // [lag_coef, intercept]
        } catch (Exception e) {
            // 奇异矩阵降级：添加微小扰动
            for (int i = 0; i < sampleSize; i++) {
                x[i][0] += 1e-10 * (i + 1);
            }
            ols.newSampleData(y, x);
            coefficients = ols.estimateRegressionParameters();
        }
        // AR 系数稳定性约束：|coef[0]| 必须 < 1 才能保证递推不发散
        // OLS 在数据稀疏时常拟合出 |coef[0]| > 1（甚至 > 100），导致递推指数爆炸（曾观测到 7e50 溢出）
        coefficients[0] = clamp(coefficients[0], -0.95, 0.95);

        // Step 3: 残差序列 → MA(1) 二次拟合
        double[] residuals = new double[n];
        for (int t = 1; t < n; t++) {
            double predicted = coefficients[0] * diffed[t - 1] + coefficients[1];
            residuals[t] = diffed[t] - predicted;
        }

        // 尝试 AR+MA 二次拟合，奇异时降级为仅 AR
        double[] arOnlyCoeffs = coefficients.clone();
        try {
            double[][] x2 = new double[sampleSize][3]; // [lag, residual_lag, intercept]
            for (int t = 1; t < n; t++) {
                x2[t - 1][0] = diffed[t - 1] + 1e-10 * (t % 3);  // AR (微扰动防共线)
                x2[t - 1][1] = residuals[t - 1] + 1e-10 * ((t + 1) % 3); // MA
                x2[t - 1][2] = 1.0;             // intercept
            }
            OLSMultipleLinearRegression ols2 = new OLSMultipleLinearRegression();
            ols2.newSampleData(y, x2);
            coefficients = ols2.estimateRegressionParameters();
        } catch (Exception e) {
            log.warn("AR+MA 拟合异常（奇异矩阵），降级纯 AR: {}", e.getMessage());
            coefficients = new double[]{arOnlyCoeffs[0], 0.0, arOnlyCoeffs[1]};
        }
        // 二次拟合后再次约束 AR 与 MA 系数稳定性（防溢出）
        coefficients[0] = clamp(coefficients[0], -0.95, 0.95);
        coefficients[1] = clamp(coefficients[1], -0.95, 0.95);

        // 历史最大值，用于递推时的预测值合理性裁剪
        double histMax = 0;
        for (double v : history) histMax = Math.max(histMax, v);
        // 预测增量上界：差分域单步变化幅度不应超过历史最大值（保守）
        double diffBound = Math.max(1.0, histMax);

        // Step 4: 递推预测（差分域），含上界裁剪防发散
        // 注意：invertDifference 以 original.length 为基准，需留足空间
        double[] diffedExtended = Arrays.copyOf(diffed, history.length + steps);
        double[] residExtended = Arrays.copyOf(residuals, history.length + steps);
        for (int h = 0; h < steps; h++) {
            int t = n + h;
            double pred = coefficients[0] * diffedExtended[t - 1]  // AR
                    + coefficients[1] * residExtended[t - 1]        // MA
                    + coefficients[2];                               // intercept
            // 差分域单步上界裁剪：|增量| 不超过历史最大值（保守，防 NaN/Infinity 与异常放大）
            if (!Double.isFinite(pred)) pred = 0;
            pred = clamp(pred, -diffBound, diffBound);
            diffedExtended[t] = pred;
            residExtended[t] = 0;
        }

        // Step 5: 逆差分还原到原始尺度
        return invertDifference(history, diffedExtended, steps);
    }

    /** 数值裁剪（min ≤ v ≤ max）. */
    private static double clamp(double v, double min, double max) {
        if (Double.isNaN(v)) return 0;
        return Math.max(min, Math.min(max, v));
    }

    private double[] difference(double[] series) {
        double[] result = new double[series.length - 1];
        for (int i = 0; i < result.length; i++) {
            result[i] = series[i + 1] - series[i];
        }
        return result;
    }

    /**
     * 逆差分还原（d=1）.
     */
    private double[] invertDifference(double[] original, double[] diffedExtended, int steps) {
        double[] result = new double[steps];
        int n = original.length;
        double[] base = Arrays.copyOf(original, n + steps);
        for (int h = 0; h < steps; h++) {
            base[n + h] = base[n + h - 1] + diffedExtended[n + h];
            result[h] = Math.max(0, base[n + h]);
        }
        return result;
    }
}
