package com.library.common.result;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * 分页结果封装.
 * <p>
 * 配合 {@link com.library.common.dto.PageDTO} 使用，封装分页查询结果。
 * MyBatis-Plus {@code IPage} 转换工具方法位于 {@code library-core} 模块的
 * {@code PageUtils} 中，避免 common 模块对 ORM 框架的直接依赖。
 *
 * @param <T> 记录类型
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PageResult<T> {

    /** 当前页数据列表 */
    private List<T> records;
    /** 总记录数 */
    private long total;
    /** 当前页码（从 1 开始） */
    private int pageNum;
    /** 每页记录数 */
    private int pageSize;
    /** 总页数 */
    private int totalPages;

    /**
     * 构建空分页结果.
     *
     * @param pageNum 当前页码
     * @param pageSize 每页记录数
     * @param <T> 记录类型
     * @return 空的 PageResult
     */
    public static <T> PageResult<T> empty(int pageNum, int pageSize) {
        PageResult<T> result = new PageResult<>();
        result.records = Collections.emptyList();
        result.total = 0;
        result.pageNum = pageNum;
        result.pageSize = pageSize;
        result.totalPages = 0;
        return result;
    }

    /**
     * 手动构建.
     *
     * @param records 数据列表
     * @param total 总记录数
     * @param pageNum 当前页码
     * @param pageSize 每页记录数
     * @param <T> 记录类型
     * @return PageResult
     */
    public static <T> PageResult<T> of(List<T> records, long total, int pageNum, int pageSize) {
        PageResult<T> result = new PageResult<>();
        result.records = records;
        result.total = total;
        result.pageNum = pageNum;
        result.pageSize = pageSize;
        result.totalPages = pageSize > 0
                ? (int) Math.ceil((double) total / pageSize)
                : 0;
        return result;
    }
}
