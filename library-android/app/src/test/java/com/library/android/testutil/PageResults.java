package com.library.android.testutil;

import com.library.android.model.PageResult;

import java.util.List;

/**
 * 分页测试 helper — 简化 PageResult 构造（避免每个测试反复重复 5 参数）.
 */
public final class PageResults {

    private PageResults() {}

    public static <T> PageResult<T> single(List<T> records) {
        return new PageResult<>(records, records.size(), 1, 20, 1);
    }

    public static <T> PageResult<T> of(List<T> records, int pageNum, int totalPages) {
        return new PageResult<>(records, records.size(), pageNum, 20, totalPages);
    }
}
