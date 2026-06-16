package com.library.core.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 管理端用户视图对象.
 * <p>
 * 供管理员查看用户列表使用。手机号和邮箱已脱敏处理，<b>不含</b> {@code passwordHash}。
 * 统计字段 {@code currentBorrows} / {@code totalOverdue} 在 Phase 4 实现前暂为 0。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserManageVO {

    /** 用户 ID */
    private Long id;

    /** 用户名/学号/工号 */
    private String username;

    /** 真实姓名 */
    private String realName;

    /** 角色 */
    private String role;

    /** 脱敏邮箱（如：t***@university.edu.cn） */
    private String email;

    /** 脱敏手机号（如：138****1234） */
    private String phone;

    /** 最大可借数量 */
    private Integer maxBooks;

    /** 账户状态 */
    private String status;

    /** 当前在借数量（Phase 4 实现） */
    @Builder.Default
    private Integer currentBorrows = 0;

    /** 历史超期次数（Phase 4 实现） */
    @Builder.Default
    private Integer totalOverdue = 0;

    /** 创建时间 */
    private LocalDateTime createTime;
}
