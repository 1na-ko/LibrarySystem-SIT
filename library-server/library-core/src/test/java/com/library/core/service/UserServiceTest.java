package com.library.core.service;

import com.library.common.exception.BizException;
import com.library.core.dto.UpdateUserDTO;
import com.library.core.entity.SysUser;
import com.library.core.enums.RoleEnum;
import com.library.core.enums.UserStatusEnum;
import com.library.core.mapper.SysUserMapper;
import com.library.core.service.impl.UserServiceImpl;
import com.library.core.vo.UserManageVO;
import com.library.core.vo.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UserService 单元测试.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@DisplayName("UserService")
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private SysUserMapper sysUserMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private SysUser user;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        user = new SysUser();
        user.setId(1L);
        user.setUsername("zhangsan");
        user.setPasswordHash("$2a$12$hashed...");
        user.setRealName("张三");
        user.setRole(RoleEnum.STUDENT);
        user.setEmail("zhangsan@university.edu.cn");
        user.setPhone("13812341234");
        user.setMaxBooks(5);
        user.setStatus(UserStatusEnum.ACTIVE);
        user.setDeleted(0);
        user.setCreateTime(now);
        user.setUpdateTime(now);
    }

    @Nested
    @DisplayName("getProfile")
    class GetProfile {

        @Test
        @DisplayName("用户存在时应返回 UserProfile（不含 passwordHash）")
        void shouldReturnProfileWithoutPasswordHash() {
            when(sysUserMapper.selectById(1L)).thenReturn(user);

            UserProfile profile = userService.getProfile(1L);

            assertThat(profile.getUsername()).isEqualTo("zhangsan");
            assertThat(profile.getEmail()).isEqualTo("zhangsan@university.edu.cn");
            assertThat(profile.getRole()).isEqualTo(RoleEnum.STUDENT);
            // 确认不暴露 passwordHash：UserProfile 类本身没有 passwordHash 字段
        }

        @Test
        @DisplayName("用户不存在时应抛出 USER_NOT_FOUND")
        void shouldThrowBizExceptionWhenUserNotFound() {
            when(sysUserMapper.selectById(999L)).thenReturn(null);

            assertThatThrownBy(() -> userService.getProfile(999L))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("用户不存在");
        }
    }

    @Nested
    @DisplayName("updateProfile")
    class UpdateProfile {

        @Test
        @DisplayName("用户存在时应更新 email 和 phone")
        void shouldUpdateEmailAndPhone() {
            UpdateUserDTO dto = new UpdateUserDTO();
            dto.setEmail("newemail@university.edu.cn");
            dto.setPhone("13987654321");

            when(sysUserMapper.selectById(1L)).thenReturn(user);
            when(sysUserMapper.updateById(any())).thenReturn(1);

            userService.updateProfile(1L, dto);

            verify(sysUserMapper).updateById(any());
        }

        @Test
        @DisplayName("用户不存在时应抛出 USER_NOT_FOUND")
        void shouldThrowBizExceptionWhenUserNotFoundForUpdate() {
            when(sysUserMapper.selectById(999L)).thenReturn(null);

            UpdateUserDTO dto = new UpdateUserDTO();
            dto.setEmail("test@university.edu.cn");
            dto.setPhone("13800000000");

            assertThatThrownBy(() -> userService.updateProfile(999L, dto))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("用户不存在");
        }
    }

    @Nested
    @DisplayName("getManageVO")
    class GetManageVO {

        @Test
        @DisplayName("应返回脱敏后的邮箱和手机号")
        void shouldMaskEmailAndPhone() {
            when(sysUserMapper.selectById(1L)).thenReturn(user);

            UserManageVO vo = userService.getManageVO(1L);

            assertThat(vo.getUsername()).isEqualTo("zhangsan");
            // 邮箱脱敏：zhangsan@university.edu.cn → z***@university.edu.cn
            assertThat(vo.getEmail()).isEqualTo("z***@university.edu.cn");
            // 手机脱敏：13812341234 → 138****1234
            assertThat(vo.getPhone()).isEqualTo("138****1234");
        }
    }
}
