package com.library.security.service;

import com.library.common.exception.BizException;
import com.library.common.exception.ErrorCode;
import com.library.core.dto.LoginRequest;
import com.library.core.dto.RegisterRequest;
import com.library.core.entity.SysUser;
import com.library.core.enums.RoleEnum;
import com.library.core.enums.UserStatusEnum;
import com.library.core.mapper.SysUserMapper;
import com.library.core.vo.LoginResponse;
import com.library.core.vo.RefreshResponse;
import com.library.security.jwt.JwtUtils;
import com.library.security.token.TokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AuthService 单元测试.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 认证业务")
class AuthServiceTest {

    @Mock
    private SysUserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private TokenService tokenService;

    @InjectMocks
    private AuthService authService;

    private SysUser buildUser(RoleEnum role, UserStatusEnum status) {
        SysUser u = new SysUser();
        u.setId(1L);
        u.setUsername("alice");
        u.setPasswordHash("$2a$12$hash");
        u.setRealName("Alice");
        u.setEmail("alice@edu.cn");
        u.setRole(role);
        u.setStatus(status);
        u.setMaxBooks(role.getDefaultMaxBooks());
        return u;
    }

    // ==================== register ====================

    @Nested
    @DisplayName("register 注册")
    class Register {

        @Test
        @DisplayName("注册成功应签发令牌对并存储 RT")
        void shouldIssueTokensOnSuccess() {
            RegisterRequest req = new RegisterRequest("2024001001", "Abc@123456", "张三", "zs@edu.cn", null);
            when(userMapper.selectCount(any())).thenReturn(0L);
            when(passwordEncoder.encode("Abc@123456")).thenReturn("$2a$12$encoded");
            // insert 回填 id
            doAnswer(inv -> {
                ((SysUser) inv.getArgument(0)).setId(1L);
                return 1;
            }).when(userMapper).insert(any(SysUser.class));
            when(jwtUtils.generateAccessToken(eq(1L), eq("2024001001"), eq("STUDENT"))).thenReturn("access");
            when(jwtUtils.generateRefreshToken(1L)).thenReturn(new JwtUtils.RefreshTokenData("rt", "jti"));
            when(jwtUtils.getAccessTokenExpiresInSeconds()).thenReturn(7200L);

            LoginResponse resp = authService.register(req);

            assertThat(resp.getAccessToken()).isEqualTo("access");
            assertThat(resp.getRefreshToken()).isEqualTo("rt");
            assertThat(resp.getTokenType()).isEqualTo("Bearer");
            assertThat(resp.getExpiresIn()).isEqualTo(7200);
            assertThat(resp.getUser().getRole()).isEqualTo(RoleEnum.STUDENT);
            verify(tokenService).storeRefresh(1L, "jti");
        }

        @Test
        @DisplayName("注册用户名已存在应抛 USERNAME_EXISTS")
        void shouldThrowWhenUsernameExists() {
            RegisterRequest req = new RegisterRequest("2024001001", "Abc@123456", "张三", "zs@edu.cn", null);
            when(userMapper.selectCount(any())).thenReturn(1L);

            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(BizException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.USERNAME_EXISTS);
            verify(userMapper, never()).insert(any());
        }

        @Test
        @DisplayName("注册应使用 BCrypt 哈希密码，不存明文")
        void shouldHashPasswordOnRegister() {
            RegisterRequest req = new RegisterRequest("newuser", "Abc@123456", "李四", "ls@edu.cn", null);
            when(userMapper.selectCount(any())).thenReturn(0L);
            when(passwordEncoder.encode("Abc@123456")).thenReturn("$2a$12$encoded");
            doAnswer(inv -> {
                ((SysUser) inv.getArgument(0)).setId(2L);
                return 1;
            }).when(userMapper).insert(any(SysUser.class));
            when(jwtUtils.generateAccessToken(anyLong(), any(), any())).thenReturn("a");
            when(jwtUtils.generateRefreshToken(anyLong())).thenReturn(new JwtUtils.RefreshTokenData("r", "j"));
            when(jwtUtils.getAccessTokenExpiresInSeconds()).thenReturn(7200L);

            authService.register(req);

            ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
            verify(userMapper).insert(captor.capture());
            assertThat(captor.getValue().getPasswordHash()).isEqualTo("$2a$12$encoded");
            assertThat(captor.getValue().getPasswordHash()).isNotEqualTo("Abc@123456");
        }
    }

    // ==================== login ====================

    @Nested
    @DisplayName("login 登录")
    class Login {

        @Test
        @DisplayName("登录成功应返回令牌对")
        void shouldReturnTokensOnSuccess() {
            SysUser user = buildUser(RoleEnum.STUDENT, UserStatusEnum.ACTIVE);
            when(userMapper.selectOne(any())).thenReturn(user);
            when(passwordEncoder.matches("Abc@123456", "$2a$12$hash")).thenReturn(true);
            when(jwtUtils.generateAccessToken(1L, "alice", "STUDENT")).thenReturn("access");
            when(jwtUtils.generateRefreshToken(1L)).thenReturn(new JwtUtils.RefreshTokenData("rt", "jti"));
            when(jwtUtils.getAccessTokenExpiresInSeconds()).thenReturn(7200L);

            LoginResponse resp = authService.login(new LoginRequest("alice", "Abc@123456"));

            assertThat(resp.getAccessToken()).isEqualTo("access");
            verify(tokenService).storeRefresh(1L, "jti");
        }

        @Test
        @DisplayName("用户不存在应抛 BAD_CREDENTIALS（防枚举）")
        void shouldThrowBadCredentialsWhenUserNotFound() {
            when(userMapper.selectOne(any())).thenReturn(null);

            assertThatThrownBy(() -> authService.login(new LoginRequest("ghost", "pwd")))
                    .isInstanceOf(BizException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.BAD_CREDENTIALS);
            verify(passwordEncoder, never()).matches(any(), any());
        }

        @Test
        @DisplayName("密码错误应抛 BAD_CREDENTIALS（防枚举）")
        void shouldThrowBadCredentialsWhenPasswordWrong() {
            SysUser user = buildUser(RoleEnum.STUDENT, UserStatusEnum.ACTIVE);
            when(userMapper.selectOne(any())).thenReturn(user);
            when(passwordEncoder.matches("wrong", "$2a$12$hash")).thenReturn(false);

            assertThatThrownBy(() -> authService.login(new LoginRequest("alice", "wrong")))
                    .isInstanceOf(BizException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.BAD_CREDENTIALS);
        }

        @Test
        @DisplayName("禁用账户应抛 USER_DISABLED")
        void shouldThrowWhenDisabled() {
            SysUser user = buildUser(RoleEnum.STUDENT, UserStatusEnum.DISABLED);
            when(userMapper.selectOne(any())).thenReturn(user);
            when(passwordEncoder.matches(any(), any())).thenReturn(true);

            assertThatThrownBy(() -> authService.login(new LoginRequest("alice", "pwd")))
                    .extracting("errorCode").isEqualTo(ErrorCode.USER_DISABLED);
        }

        @Test
        @DisplayName("冻结账户应抛 ACCOUNT_FROZEN")
        void shouldThrowWhenFrozen() {
            SysUser user = buildUser(RoleEnum.STUDENT, UserStatusEnum.FROZEN);
            when(userMapper.selectOne(any())).thenReturn(user);
            when(passwordEncoder.matches(any(), any())).thenReturn(true);

            assertThatThrownBy(() -> authService.login(new LoginRequest("alice", "pwd")))
                    .extracting("errorCode").isEqualTo(ErrorCode.ACCOUNT_FROZEN);
        }
    }

    // ==================== refresh ====================

    @Nested
    @DisplayName("refresh 刷新")
    class Refresh {

        @Test
        @DisplayName("刷新成功应返回新令牌对并轮换 RT")
        void shouldRotateOnSuccess() {
            Claims claims = org.mockito.Mockito.mock(Claims.class);
            when(claims.getSubject()).thenReturn("1");
            when(claims.getId()).thenReturn("old-jti");
            when(jwtUtils.parse("rt")).thenReturn(claims);
            when(jwtUtils.isRefresh(claims)).thenReturn(true);
            when(jwtUtils.generateRefreshToken(1L)).thenReturn(new JwtUtils.RefreshTokenData("new-rt", "new-jti"));
            when(tokenService.rotate(1L, "old-jti", "new-jti")).thenReturn(0);
            SysUser user = buildUser(RoleEnum.STUDENT, UserStatusEnum.ACTIVE);
            when(userMapper.selectById(1L)).thenReturn(user);
            when(jwtUtils.generateAccessToken(1L, "alice", "STUDENT")).thenReturn("new-access");
            when(jwtUtils.getAccessTokenExpiresInSeconds()).thenReturn(7200L);

            RefreshResponse resp = authService.refresh("rt");

            assertThat(resp.getAccessToken()).isEqualTo("new-access");
            assertThat(resp.getRefreshToken()).isEqualTo("new-rt");
            assertThat(resp.getExpiresIn()).isEqualTo(7200);
        }

        @Test
        @DisplayName("RT 过期应抛 TOKEN_EXPIRED")
        void shouldThrowWhenExpired() {
            when(jwtUtils.parse("rt")).thenThrow(new ExpiredJwtException(null, null, "expired"));

            assertThatThrownBy(() -> authService.refresh("rt"))
                    .extracting("errorCode").isEqualTo(ErrorCode.TOKEN_EXPIRED);
        }

        @Test
        @DisplayName("RT 无效应抛 TOKEN_INVALID")
        void shouldThrowWhenInvalid() {
            when(jwtUtils.parse("rt")).thenThrow(new JwtException("bad") {
            });

            assertThatThrownBy(() -> authService.refresh("rt"))
                    .extracting("errorCode").isEqualTo(ErrorCode.TOKEN_INVALID);
        }

        @Test
        @DisplayName("非 refresh 类型应抛 TOKEN_INVALID")
        void shouldThrowWhenTypeMismatch() {
            Claims claims = org.mockito.Mockito.mock(Claims.class);
            when(jwtUtils.parse("rt")).thenReturn(claims);
            when(jwtUtils.isRefresh(claims)).thenReturn(false);

            assertThatThrownBy(() -> authService.refresh("rt"))
                    .extracting("errorCode").isEqualTo(ErrorCode.TOKEN_INVALID);
        }

        @Test
        @DisplayName("重放（轮换返回 1）应抛 TOKEN_INVALID")
        void shouldThrowWhenReplayDetected() {
            Claims claims = org.mockito.Mockito.mock(Claims.class);
            when(claims.getSubject()).thenReturn("1");
            when(claims.getId()).thenReturn("old-jti");
            when(jwtUtils.parse("rt")).thenReturn(claims);
            when(jwtUtils.isRefresh(claims)).thenReturn(true);
            when(jwtUtils.generateRefreshToken(1L)).thenReturn(new JwtUtils.RefreshTokenData("new-rt", "new-jti"));
            when(tokenService.rotate(1L, "old-jti", "new-jti")).thenReturn(1);

            assertThatThrownBy(() -> authService.refresh("rt"))
                    .extracting("errorCode").isEqualTo(ErrorCode.TOKEN_INVALID);
        }
    }

    // ==================== logout ====================

    @Test
    @DisplayName("logout 应撤销当前用户 RT")
    void shouldRevokeOnLogout() {
        authService.logout(42L);
        verify(tokenService).revoke(42L);
    }
}
