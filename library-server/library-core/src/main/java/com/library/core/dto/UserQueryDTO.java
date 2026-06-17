package com.library.core.dto;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.common.exception.BizException;
import com.library.common.exception.ErrorCode;
import com.library.core.entity.SysUser;
import com.library.core.enums.RoleEnum;
import com.library.core.enums.UserStatusEnum;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

/**
 * 用户列表查询 DTO.
 * <p>
 * 支持按角色、状态、用户名/姓名筛选。分页参数默认 page=1, size=20。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
public class UserQueryDTO {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    /** 角色筛选（枚举名，如 STUDENT） */
    private String role;
    /** 状态筛选（枚举名，如 ACTIVE） */
    private String status;
    /** 关键词搜索（匹配 username 或 realName） */
    private String keyword;
    /** 页码（1-based） */
    private int pageNum = DEFAULT_PAGE;
    /** 每页大小（上限 100） */
    private int pageSize = DEFAULT_SIZE;

    /**
     * 构造方法（兼容 Controller 整型参数）.
     */
    public UserQueryDTO(String role, String status, String keyword, int page, int size) {
        this.role = role;
        this.status = status;
        this.keyword = keyword;
        this.pageNum = Math.max(page, 1);
        this.pageSize = Math.min(Math.max(size, 1), MAX_SIZE);
    }

    /**
     * 构建 MyBatis-Plus 查询条件.
     */
    public LambdaQueryWrapper<SysUser> buildWrapper() {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(role)) {
            wrapper.eq(SysUser::getRole, safeRoleEnum(role));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(SysUser::getStatus, safeStatusEnum(status));
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getRealName, keyword));
        }
        wrapper.orderByAsc(SysUser::getId);
        return wrapper;
    }

    private RoleEnum safeRoleEnum(String value) {
        try {
            return RoleEnum.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BizException(ErrorCode.BAD_REQUEST, "无效的角色筛选值: " + value);
        }
    }

    private UserStatusEnum safeStatusEnum(String value) {
        try {
            return UserStatusEnum.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BizException(ErrorCode.BAD_REQUEST, "无效的状态筛选值: " + value);
        }
    }
}
