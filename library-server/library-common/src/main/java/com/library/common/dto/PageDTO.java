package com.library.common.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 公共分页请求 DTO.
 * <p>
 * 所有分页查询接口统一使用此 DTO 接收分页参数。
 * 默认 pageNum=1, pageSize=20，最大 pageSize=100。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
public class PageDTO {

    /** 每页记录数硬上限（防御性，与 @Max 一致） */
    private static final int MAX_PAGE_SIZE = 100;

    /** 当前页码（从 1 开始） */
    @Min(value = 1, message = "页码最小为 1")
    private int pageNum = 1;

    /** 每页记录数 */
    @Min(value = 1, message = "每页最少 1 条")
    @Max(value = 100, message = "每页最多 100 条")
    private int pageSize = 20;

    /**
     * 带参构造器：对入参做防御性钳制.
     * <p>
     * 当 Controller 以 {@code @RequestParam} 接收分页参数后手动 {@code new PageDTO(pageNum, pageSize)}
     * 构造时，Bean Validation 注解不会触发（未走 {@code @Valid} 绑定路径）。此构造器在构造期
     * 钳制非法值，确保 pageNum ≥ 1、1 ≤ pageSize ≤ 100，避免超大 pageSize 导致的 DoS
     * 或负 offset 触发 SQL 异常。一处钳制，全局生效。
     *
     * @param pageNum  页码（小于 1 时钳制为 1）
     * @param pageSize 每页大小（小于 1 钳制为 1，大于 100 钳制为 100）
     */
    public PageDTO(int pageNum, int pageSize) {
        this.pageNum = Math.max(1, pageNum);
        this.pageSize = Math.max(1, Math.min(MAX_PAGE_SIZE, pageSize));
    }

    /**
     * 获取偏移量（用于 SQL LIMIT offset, size）.
     *
     * @return 偏移量（经构造期钳制保证非负）
     */
    public int getOffset() {
        return (pageNum - 1) * pageSize;
    }
}
