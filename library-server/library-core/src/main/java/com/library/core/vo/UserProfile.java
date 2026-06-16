package com.library.core.vo;

import com.library.core.enums.RoleEnum;
import com.library.core.enums.UserStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户信息 VO.
 * <p>
 * 对应 OpenAPI {@code UserProfile} schema，登录响应与个人中心共用。
 * 注意：{@code role}/{@code status} 用枚举类型，Jackson 默认序列化为枚举 name()，与契约 enum 值一致。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {

    /** 用户 ID */
    private Long id;

    /** 用户名 */
    private String username;

    /** 真实姓名 */
    private String realName;

    /** 角色 */
    private RoleEnum role;

    /** 邮箱 */
    private String email;

    /** 手机号 */
    private String phone;

    /** 最大可借数量 */
    private Integer maxBooks;

    /** 账户状态 */
    private UserStatusEnum status;

    /** 创建时间 */
    private LocalDateTime createTime;
}
