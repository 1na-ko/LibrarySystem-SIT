package com.library.common.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Collection;

/**
 * 字符串工具类扩展.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StringUtils {

    /** 空字符串 */
    public static final String EMPTY = "";

    /**
     * 判断字符串是否有文本内容（非 null 且非空白）.
     */
    public static boolean hasText(String str) {
        return str != null && !str.isBlank();
    }

    /**
     * 判断字符串是否为空或空白.
     */
    public static boolean isBlank(String str) {
        return str == null || str.isBlank();
    }

    /**
     * 截断字符串到指定长度，超出部分用 "..." 表示.
     */
    public static String truncate(String str, int maxLength) {
        if (str == null || str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }

    /**
     * 将集合的字符串元素用分隔符连接.
     */
    public static String join(Collection<String> items, String delimiter) {
        if (items == null || items.isEmpty()) {
            return EMPTY;
        }
        return String.join(delimiter, items);
    }
}
