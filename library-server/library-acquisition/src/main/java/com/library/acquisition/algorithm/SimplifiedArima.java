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
     * 预测未来值.
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

        // Step 4: 递推预测（差分域）
        // 注意：invertDifference 以 original.length 为基准，需留足空间
        double[] diffedExtended = Arrays.copyOf(diffed, history.length + steps);
        double[] residExtended = Arrays.copyOf(residuals, history.length + steps);
        for (int h = 0; h < steps; h++) {
            int t = n + h;
            double pred = coefficients[0] * diffedExtended[t - 1]  // AR
                    + coefficients[1] * residExtended[t - 1]        // MA
                    + coefficients[2];                               // intercept
            diffedExtended[t] = Math.max(0, pred);
            residExtended[t] = 0;
        }

        // Step 5: 逆差分还原到原始尺度
        return invertDifference(history, diffedExtended, steps);
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
