package com.library.core.service.impl;

import com.library.common.exception.BizException;
import com.library.common.exception.ErrorCode;
import com.library.core.dto.UpdateUserDTO;
import com.library.core.entity.SysUser;
import com.library.core.mapper.SysUserMapper;
import com.library.core.service.UserService;
import com.library.core.vo.UserManageVO;
import com.library.core.vo.UserProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户基础服务实现.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SysUserMapper sysUserMapper;

    @Override
    public UserProfile getProfile(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        return toProfile(user);
    }

    @Override
    @Transactional
    public void updateProfile(Long userId, UpdateUserDTO dto) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }

        SysUser update = new SysUser();
        update.setId(userId);
        update.setEmail(dto.getEmail());
        update.setPhone(dto.getPhone());
        sysUserMapper.updateById(update);

        log.debug("用户 {} 更新个人信息成功", userId);
    }

    @Override
    public UserManageVO getManageVO(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        return toManageVO(user);
    }

    /**
     * Entity → UserProfile（不含 passwordHash）.
     */
    private UserProfile toProfile(SysUser user) {
        return UserProfile.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .role(user.getRole())
                .email(user.getEmail())
                .phone(user.getPhone())
                .maxBooks(user.getMaxBooks())
                .status(user.getStatus())
                .createTime(user.getCreateTime())
                .build();
    }

    /**
     * Entity → UserManageVO（含脱敏）.
     */
    private UserManageVO toManageVO(SysUser user) {
        return UserManageVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .email(maskEmail(user.getEmail()))
                .phone(maskPhone(user.getPhone()))
                .maxBooks(user.getMaxBooks())
                .status(user.getStatus() != null ? user.getStatus().name() : null)
                .createTime(user.getCreateTime())
                .build();
    }

    /**
     * 邮箱脱敏：保留首字符和 @ 后域名，中间替换为 ***.
     * <p>
     * 例：test@university.edu.cn → t***@university.edu.cn
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return "*" + email.substring(atIndex);
        }
        return email.charAt(0) + "***" + email.substring(atIndex);
    }

    /**
     * 手机号脱敏：保留前 3 后 4，中间替换为 ****.
     * <p>
     * 例：13812341234 → 138****1234
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
