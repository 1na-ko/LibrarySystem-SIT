package com.library.common.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * StringUtils 工具类单元测试.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@DisplayName("StringUtils")
class StringUtilsTest {

    @Nested
    @DisplayName("hasText")
    class HasText {

        @Test
        @DisplayName("普通文本应返回 true")
        void shouldReturnTrueWhenNormalText() {
            assertThat(StringUtils.hasText("hello")).isTrue();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  ", "\t", "\n"})
        @DisplayName("null/空/空白应返回 false")
        void shouldReturnFalseWhenBlank(String input) {
            assertThat(StringUtils.hasText(input)).isFalse();
        }
    }

    @Nested
    @DisplayName("isBlank")
    class IsBlank {

        @Test
        @DisplayName("普通文本应返回 false")
        void shouldReturnFalseWhenNormalText() {
            assertThat(StringUtils.isBlank("hello")).isFalse();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  ", "\t", "\n"})
        @DisplayName("null/空/空白应返回 true")
        void shouldReturnTrueWhenBlank(String input) {
            assertThat(StringUtils.isBlank(input)).isTrue();
        }
    }

    @Nested
    @DisplayName("truncate")
    class Truncate {

        @Test
        @DisplayName("null 应返回 null")
        void shouldReturnNullWhenNull() {
            assertThat(StringUtils.truncate(null, 10)).isNull();
        }

        @Test
        @DisplayName("短于 maxLength 应原样返回")
        void shouldReturnOriginalWhenShorterThanMax() {
            assertThat(StringUtils.truncate("hello", 10)).isEqualTo("hello");
        }

        @Test
        @DisplayName("恰好等于 maxLength 应原样返回")
        void shouldReturnOriginalWhenEqualToMax() {
            assertThat(StringUtils.truncate("hello", 5)).isEqualTo("hello");
        }

        @Test
        @DisplayName("超过 maxLength 应截断并补 '...'")
        void shouldTruncateWithEllipsisWhenTooLong() {
            assertThat(StringUtils.truncate("hello world", 8))
                    .isEqualTo("hello...");
        }
    }

    @Nested
    @DisplayName("join")
    class Join {

        @Test
        @DisplayName("null 集合应返回空字符串")
        void shouldReturnEmptyWhenNull() {
            assertThat(StringUtils.join(null, ", ")).isEmpty();
        }

        @Test
        @DisplayName("空集合应返回空字符串")
        void shouldReturnEmptyWhenEmptyList() {
            assertThat(StringUtils.join(Collections.emptyList(), ", ")).isEmpty();
        }

        @Test
        @DisplayName("单个元素不应包含分隔符")
        void shouldReturnSingleElementWithoutDelimiter() {
            List<String> list = Collections.singletonList("Java");
            assertThat(StringUtils.join(list, ", ")).isEqualTo("Java");
        }

        @Test
        @DisplayName("多元素应以分隔符连接")
        void shouldJoinWithDelimiter() {
            List<String> list = Arrays.asList("Java", "Python", "Go");
            assertThat(StringUtils.join(list, ", ")).isEqualTo("Java, Python, Go");
        }
    }

    @Test
    @DisplayName("EMPTY 常量应为空字符串")
    void shouldBeEmptyStringConstant() {
        assertThat(StringUtils.EMPTY).isEmpty();
    }
}
