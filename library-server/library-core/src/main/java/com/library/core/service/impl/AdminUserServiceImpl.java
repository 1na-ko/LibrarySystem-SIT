package com.library.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.common.exception.BizException;
import com.library.common.exception.ErrorCode;
import com.library.common.result.PageResult;
import com.library.core.dto.UserQueryDTO;
import com.library.core.dto.UserStatusUpdateDTO;
import com.library.core.entity.SysUser;
import com.library.core.enums.UserStatusEnum;
import com.library.core.mapper.BorrowRecordMapper;
import com.library.core.mapper.SysUserMapper;
import com.library.core.service.AdminUserService;
import com.library.core.vo.UserManageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理员用户管理 Service 实现.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final SysUserMapper sysUserMapper;
    private final BorrowRecordMapper borrowRecordMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResult<UserManageVO> listUsers(UserQueryDTO query) {
        LambdaQueryWrapper<SysUser> wrapper = query.buildWrapper();
        Page<SysUser> mpPage = new Page<>(query.getPageNum(), query.getPageSize());
        Page<SysUser> result = sysUserMapper.selectPage(mpPage, wrapper);

        // 批量查询借阅统计
        List<Long> userIds = result.getRecords().stream()
                .map(SysUser::getId)
                .collect(Collectors.toList());
        Map<Long, Long> currentBorrowsMap = userIds.isEmpty()
                ? Collections.emptyMap()
                : borrowRecordMapper.countCurrentBorrowsByUserIds(userIds).stream()
                        .collect(Collectors.toMap(
                                m -> (Long) m.get("user_id"),
                                m -> (Long) m.get("cnt"),
                                (a, b) -> a));
        Map<Long, Long> overdueMap = userIds.isEmpty()
                ? Collections.emptyMap()
                : borrowRecordMapper.countOverdueByUserIds(userIds).stream()
                        .collect(Collectors.toMap(
                                m -> (Long) m.get("user_id"),
                                m -> (Long) m.get("cnt"),
                                (a, b) -> a));

        List<UserManageVO> voList = result.getRecords().stream()
                .map(user -> toManageVO(user, currentBorrowsMap, overdueMap))
                .collect(Collectors.toList());

        return PageResult.of(voList, result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    @Override
    @Transactional
    public void updateStatus(Long operatorId, Long userId, UserStatusUpdateDTO dto) {
        SysUser target = sysUserMapper.selectById(userId);
        if (target == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }

        // 防自操作
        if (target.getId().equals(operatorId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "不能修改自己的账户状态");
        }

        // DISABLED 状态不可逆
        if (target.getStatus() == UserStatusEnum.DISABLED) {
            throw new BizException(ErrorCode.FORBIDDEN, "已禁用的账户不可恢复");
        }

        UserStatusEnum newStatus = dto.getStatus();
        log.info("用户状态变更: operatorId={}, targetUserId={}, {} -> {}",
                operatorId, userId, target.getStatus(), newStatus);

        // 部分更新（仅 status 和 updateTime）
        SysUser update = new SysUser();
        update.setId(userId);
        update.setStatus(newStatus);
        sysUserMapper.updateById(update);
    }

    /**
     * 将 SysUser 转换为 UserManageVO（含统计信息）.
     */
    private UserManageVO toManageVO(SysUser user, Map<Long, Long> currentBorrowsMap,
                                     Map<Long, Long> overdueMap) {
        UserManageVO vo = new UserManageVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setRole(user.getRole().name());
        vo.setEmail(maskEmail(user.getEmail()));
        vo.setPhone(maskPhone(user.getPhone()));
        vo.setMaxBooks(user.getMaxBooks());
        vo.setStatus(user.getStatus().name());
        vo.setCurrentBorrows(currentBorrowsMap.getOrDefault(user.getId(), 0L).intValue());
        vo.setTotalOverdue(overdueMap.getOrDefault(user.getId(), 0L).intValue());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }

    /**
     * 邮箱脱敏：保留首字符和 @ 后域名.
     */
    String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        int atIndex = email.indexOf('@');
        return email.charAt(0) + "***@" + email.substring(atIndex + 1);
    }

    /**
     * 手机号脱敏：保留前 3 后 4.
     */
    String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
