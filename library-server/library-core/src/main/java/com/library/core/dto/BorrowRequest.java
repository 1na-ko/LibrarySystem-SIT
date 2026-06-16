package com.library.core.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 借书请求 DTO.
 * <p>
 * 对应 OpenAPI {@code BorrowRequest} schema，仅需传图书 ID。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BorrowRequest {

    /** 图书 ID */
    @NotNull(message = "图书 ID 不能为空")
    private Long bookId;
}
