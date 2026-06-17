package com.library.core.service;

import com.library.common.result.PageResult;
import com.library.core.dto.UserQueryDTO;
import com.library.core.dto.UserStatusUpdateDTO;
import com.library.core.vo.UserManageVO;

/**
 * 管理员用户管理 Service.
 * <p>
 * 提供用户列表分页查询和用户状态变更功能，
 * 供 {@code AdminUserController}（library-security）调用。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public interface AdminUserService {

    /**
     * 分页查询用户列表（支持角色/状态/关键词筛选）.
     *
     * @param query 查询条件
     * @return 分页结果（含 enriched 统计信息）
     */
    PageResult<UserManageVO> listUsers(UserQueryDTO query);

    /**
     * 变更用户状态.
     * <p>
     * 校验规则：
     * <ul>
     *   <li>目标用户必须存在</li>
     *   <li>不能修改自己的账户状态</li>
     *   <li>DISABLED 状态不可逆</li>
     * </ul>
     *
     * @param operatorId 操作人 ID（由 Controller 从 SecurityContext 提取）
     * @param userId     目标用户 ID
     * @param dto        目标状态
     */
    void updateStatus(Long operatorId, Long userId, UserStatusUpdateDTO dto);
}
