package com.library.common.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * 日期时间工具类.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DateUtils {

    /** 标准日期格式 */
    public static final String DATE_PATTERN = "yyyy-MM-dd";
    /** 标准日期时间格式 */
    public static final String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);
    public static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern(DATETIME_PATTERN);

    /**
     * 计算两个日期之间的天数差.
     */
    public static long daysBetween(LocalDate start, LocalDate end) {
        return ChronoUnit.DAYS.between(start, end);
    }

    /**
     * 判断日期是否已过期（早于当前日期）.
     */
    public static boolean isOverdue(LocalDate date) {
        return date != null && date.isBefore(LocalDate.now());
    }

    /**
     * 计算到期日（从今天起 + N 天）.
     */
    public static LocalDate dueDateFromNow(int days) {
        return LocalDate.now().plusDays(days);
    }

    /**
     * 获取当前日期字符串.
     */
    public static String todayStr() {
        return LocalDate.now().format(DATE_FORMATTER);
    }

    /**
     * 获取当前日期时间字符串.
     */
    public static String nowStr() {
        return LocalDateTime.now().format(DATETIME_FORMATTER);
    }

    /**
     * 解析日期字符串.
     */
    public static LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        return LocalDate.parse(dateStr, DATE_FORMATTER);
    }
}
