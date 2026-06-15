package com.library.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.library.core.enums.RoleEnum;
import com.library.core.enums.UserStatusEnum;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体（对应 sys_user 表）.
 * <p>
 * 字段通过 MyBatis-Plus {@code map-underscore-to-camel-case} 自动映射下划线列名。
 * 逻辑删除列 {@code deleted} 由全局配置 {@code logic-delete-field} 处理，无需 {@code @TableLogic}。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
@TableName("sys_user")
public class SysUser {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名/学号/工号 */
    private String username;

    /** BCrypt 密码哈希 */
    private String passwordHash;

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

    /** 逻辑删除（0=未删除, 1=已删除） */
    private Integer deleted;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
