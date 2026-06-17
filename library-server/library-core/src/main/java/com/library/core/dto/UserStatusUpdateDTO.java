package com.library.core.dto;

import com.library.core.enums.UserStatusEnum;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户状态更新 DTO.
 * <p>
 * 对应 OpenAPI {@code UserStatusUpdateRequest} Schema。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserStatusUpdateDTO {

    /** 目标状态 */
    @NotNull(message = "状态不能为空")
    private UserStatusEnum status;
}
