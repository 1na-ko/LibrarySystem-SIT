package com.library.common.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
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
@AllArgsConstructor
public class PageDTO {

    /** 当前页码（从 1 开始） */
    @Min(value = 1, message = "页码最小为 1")
    private int pageNum = 1;

    /** 每页记录数 */
    @Min(value = 1, message = "每页最少 1 条")
    @Max(value = 100, message = "每页最多 100 条")
    private int pageSize = 20;

    /**
     * 获取偏移量（用于 SQL LIMIT offset, size）.
     *
     * @return 偏移量
     */
    public int getOffset() {
        return (pageNum - 1) * pageSize;
    }
}
