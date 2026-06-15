package com.library.common.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * DateUtils 工具类单元测试.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@DisplayName("DateUtils")
class DateUtilsTest {

    @Test
    @DisplayName("daysBetween 应正确计算天数差")
    void shouldCalculateDaysCorrectly() {
        LocalDate start = LocalDate.of(2026, 6, 1);
        LocalDate end = LocalDate.of(2026, 6, 15);

        assertThat(DateUtils.daysBetween(start, end)).isEqualTo(14);
    }

    @Test
    @DisplayName("daysBetween 相同日期应返回 0")
    void shouldReturnZeroWhenSameDay() {
        LocalDate date = LocalDate.of(2026, 6, 15);
        assertThat(DateUtils.daysBetween(date, date)).isZero();
    }

    @Test
    @DisplayName("daysBetween 负数表示 end 早于 start")
    void shouldReturnNegativeWhenEndBeforeStart() {
        LocalDate start = LocalDate.of(2026, 6, 15);
        LocalDate end = LocalDate.of(2026, 6, 1);

        assertThat(DateUtils.daysBetween(start, end)).isEqualTo(-14);
    }

    @Test
    @DisplayName("isOverdue 昨日应返回 true")
    void shouldBeOverdueWhenYesterday() {
        assertThat(DateUtils.isOverdue(LocalDate.now().minusDays(1))).isTrue();
    }

    @Test
    @DisplayName("isOverdue 明日应返回 false")
    void shouldNotBeOverdueWhenTomorrow() {
        assertThat(DateUtils.isOverdue(LocalDate.now().plusDays(1))).isFalse();
    }

    @Test
    @DisplayName("isOverdue 今日应返回 false（仅严格小于判断）")
    void shouldNotBeOverdueWhenToday() {
        assertThat(DateUtils.isOverdue(LocalDate.now())).isFalse();
    }

    @Test
    @DisplayName("isOverdue null 应返回 false")
    void shouldReturnFalseWhenNull() {
        assertThat(DateUtils.isOverdue(null)).isFalse();
    }

    @Test
    @DisplayName("dueDateFromNow(30) 应返回 30 天后")
    void shouldReturnDateAfter30Days() {
        LocalDate expected = LocalDate.now().plusDays(30);
        assertThat(DateUtils.dueDateFromNow(30)).isEqualTo(expected);
    }

    @Test
    @DisplayName("dueDateFromNow(0) 应返回今天")
    void shouldReturnTodayWhenZeroDays() {
        assertThat(DateUtils.dueDateFromNow(0)).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("todayStr 应返回 yyyy-MM-dd 格式的今天日期")
    void shouldReturnTodayInCorrectFormat() {
        String today = DateUtils.todayStr();

        assertThat(today).matches("\\d{4}-\\d{2}-\\d{2}");
        assertThat(today).isEqualTo(LocalDate.now().format(DateUtils.DATE_FORMATTER));
    }

    @Test
    @DisplayName("nowStr 应返回 yyyy-MM-dd HH:mm:ss 格式的当前时间")
    void shouldReturnNowInCorrectFormat() {
        String now = DateUtils.nowStr();

        assertThat(now).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
    }

    @Test
    @DisplayName("parseDate 应正确解析标准日期格式")
    void shouldParseValidDateString() {
        LocalDate date = DateUtils.parseDate("2026-06-15");

        assertThat(date).isEqualTo(LocalDate.of(2026, 6, 15));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("parseDate null 或空字符串应返回 null")
    void shouldReturnNullWhenNullOrBlank(String input) {
        assertThat(DateUtils.parseDate(input)).isNull();
    }

    @Test
    @DisplayName("parseDate 非法格式应抛异常")
    void shouldThrowExceptionWhenInvalidFormat() {
        assertThatThrownBy(() -> DateUtils.parseDate("2026/06/15"))
                .isInstanceOf(DateTimeParseException.class);
    }
}
