package com.library.core.service;

import com.library.core.dto.UpdateUserDTO;
import com.library.core.vo.UserManageVO;
import com.library.core.vo.UserProfile;

/**
 * 用户基础服务接口.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public interface UserService {

    /**
     * 获取用户资料（不含 passwordHash）.
     *
     * @param userId 用户 ID
     * @return 用户资料 VO
     * @throws com.library.common.exception.BizException 用户不存在时抛出 USER_NOT_FOUND
     */
    UserProfile getProfile(Long userId);

    /**
     * 更新用户邮箱和手机号.
     *
     * @param userId 用户 ID
     * @param dto 更新请求
     * @throws com.library.common.exception.BizException 用户不存在时抛出 USER_NOT_FOUND
     */
    void updateProfile(Long userId, UpdateUserDTO dto);

    /**
     * 获取管理端用户视图（含脱敏和统计字段）.
     *
     * @param userId 用户 ID
     * @return 管理端用户 VO
     * @throws com.library.common.exception.BizException 用户不存在时抛出 USER_NOT_FOUND
     */
    UserManageVO getManageVO(Long userId);
}
