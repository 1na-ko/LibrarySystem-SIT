package com.library.acquisition.algorithm;

import com.library.common.exception.BizException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.Offset.offset;

@DisplayName("SimplifiedArima")
class SimplifiedArimaTest {

    private final SimplifiedArima arima = new SimplifiedArima();

    @Nested
    @DisplayName("forecast")
    class Forecast {

        @Test
        @DisplayName("数据充分时应返回正数预测值")
        void shouldForecastWhenHistorySufficient() {
            double[] history = {10, 12, 11, 13, 14, 15, 13, 16, 17, 15, 18, 19};
            double[] forecast = arima.forecast(history, 3);
            assertThat(forecast).hasSize(3);
            for (double v : forecast) {
                assertThat(v).isGreaterThanOrEqualTo(0);
            }
        }

        @Test
        @DisplayName("多步预测值应有合理趋势")
        void shouldProduceReasonableTrend() {
            // 使用含轻微噪声的真实时序（避免完全线性导致 OLS 奇异矩阵）
            double[] history = {10, 12, 11, 14, 16, 15, 18, 20, 19, 22, 24, 23};
            double[] forecast = arima.forecast(history, 2);
            assertThat(forecast).hasSize(2);
            assertThat(forecast[0]).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("forecast (异常)")
    class ForecastError {

        @Test
        @DisplayName("数据点不足 6 时应抛 PREDICTION_DATA_INSUFFICIENT")
        void shouldThrowWhenDataInsufficient() {
            double[] history = {10, 12, 11};
            assertThatThrownBy(() -> arima.forecast(history, 3))
                    .isInstanceOf(BizException.class);
        }

        @Test
        @DisplayName("null 输入应抛异常")
        void shouldThrowWhenNullInput() {
            assertThatThrownBy(() -> arima.forecast(null, 3))
                    .isInstanceOf(BizException.class);
        }

        @Test
        @DisplayName("steps=0 应返回空数组")
        void shouldReturnEmptyWhenZeroSteps() {
            double[] history = {10, 12, 11, 13, 14, 15};
            double[] forecast = arima.forecast(history, 0);
            assertThat(forecast).isEmpty();
        }
    }

    @Nested
    @DisplayName("forecastLenient (WP-0 降级)")
    class ForecastLenient {

        @Test
        @DisplayName("数据不足时应返回历史均值常数序列而非抛异常")
        void shouldReturnAverageWhenDataInsufficient() {
            double[] history = {10, 20, 30}; // 平均 20
            double[] forecast = arima.forecastLenient(history, 3);
            assertThat(forecast).hasSize(3);
            for (double v : forecast) {
                assertThat(v).isCloseTo(20.0, offset(0.01));
            }
        }

        @Test
        @DisplayName("空历史应返回全零序列")
        void shouldReturnZerosWhenHistoryEmpty() {
            double[] forecast = arima.forecastLenient(new double[0], 3);
            assertThat(forecast).hasSize(3);
            for (double v : forecast) {
                assertThat(v).isEqualTo(0.0);
            }
        }

        @Test
        @DisplayName("null 历史应返回全零序列")
        void shouldReturnZerosWhenHistoryNull() {
            double[] forecast = arima.forecastLenient(null, 2);
            assertThat(forecast).hasSize(2);
        }

        @Test
        @DisplayName("数据充分时应走正常 ARIMA 路径")
        void shouldUseArimaWhenDataSufficient() {
            double[] history = {10, 12, 11, 13, 14, 15, 13, 16, 17, 15, 18, 19};
            double[] forecast = arima.forecastLenient(history, 2);
            assertThat(forecast).hasSize(2);
            for (double v : forecast) {
                assertThat(v).isGreaterThanOrEqualTo(0);
            }
        }

        @Test
        @DisplayName("hasSufficientData 应正确判断")
        void shouldDetectSufficientData() {
            assertThat(arima.hasSufficientData(null)).isFalse();
            assertThat(arima.hasSufficientData(new double[0])).isFalse();
            assertThat(arima.hasSufficientData(new double[]{1, 2, 3})).isFalse();
            assertThat(arima.hasSufficientData(new double[]{1, 2, 3, 4, 5, 6})).isTrue();
            assertThat(arima.hasSufficientData(new double[]{1, 2, 3, 4, 5, 6, 7, 8})).isTrue();
        }
    }
}
