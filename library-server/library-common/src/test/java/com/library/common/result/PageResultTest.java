package com.library.common.result;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PageResult 分页结果封装单元测试.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@DisplayName("PageResult 分页结果")
class PageResultTest {

    @Test
    @DisplayName("empty() 应返回记录数为 0、总页数为 0")
    void shouldReturnEmptyResult() {
        PageResult<String> result = PageResult.empty(1, 20);

        assertThat(result.getRecords()).isEmpty();
        assertThat(result.getTotal()).isZero();
        assertThat(result.getPageNum()).isEqualTo(1);
        assertThat(result.getPageSize()).isEqualTo(20);
        assertThat(result.getTotalPages()).isZero();
    }

    @Test
    @DisplayName("of() 应正确计算 totalPages")
    void shouldCalculateTotalPagesWhenExactDivision() {
        List<String> records = Arrays.asList("a", "b", "c");
        PageResult<String> result = PageResult.of(records, 30, 1, 10);

        assertThat(result.getRecords()).hasSize(3);
        assertThat(result.getTotal()).isEqualTo(30);
        assertThat(result.getPageSize()).isEqualTo(10);
        assertThat(result.getTotalPages()).isEqualTo(3); // ceil(30/10)
    }

    @Test
    @DisplayName("of() 非整除时 totalPages 应向上取整")
    void shouldRoundUpTotalPagesWhenNotExactDivision() {
        PageResult<String> result = PageResult.of(
                Collections.singletonList("x"), 25, 1, 10);

        assertThat(result.getTotalPages()).isEqualTo(3); // ceil(25/10) = 3
    }

    @Test
    @DisplayName("of() total 为 1、pageSize 较大时应返回 1 页")
    void shouldReturnOnePageWhenTotalLessThanPageSize() {
        PageResult<String> result = PageResult.of(
                Collections.singletonList("x"), 1, 1, 20);

        assertThat(result.getTotalPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("of() total 为 0 时应返回 0 页")
    void shouldReturnZeroPagesWhenTotalIsZero() {
        PageResult<String> result = PageResult.of(Collections.emptyList(), 0, 1, 10);

        assertThat(result.getTotalPages()).isZero();
    }

    @Test
    @DisplayName("of() 应保留传入的 pageNum 和 pageSize")
    void shouldPreservePageNumAndPageSize() {
        PageResult<String> result = PageResult.of(
                Arrays.asList("a", "b"), 100, 3, 25);

        assertThat(result.getPageNum()).isEqualTo(3);
        assertThat(result.getPageSize()).isEqualTo(25);
    }
}
