package com.library.security.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.library.core.vo.UserProfile;
import com.library.security.jwt.JwtUtils;
import com.library.security.token.TokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 认证业务服务.
 * <p>
 * 提供注册、登录、刷新、登出四个核心流程：
 * <ul>
 *   <li>register：校验用户名唯一 → BCrypt 哈希 → 默认 STUDENT/ACTIVE → 注册即登录</li>
 *   <li>login：用户不存在与密码错误统一返回 BAD_CREDENTIALS（防枚举）→ 状态校验 → 签发令牌对</li>
 *   <li>refresh：解析 RT → 类型校验 → 原子轮换（重放检测）→ 重查用户 → 签发新令牌对</li>
 *   <li>logout：删除 Redis 中的 RT 记录（Access 自然过期）</li>
 * </ul>
 * <p>
 * 注：本类置于 library-security 模块而非 core，因其强依赖 {@link JwtUtils}/{@link TokenService}
 * （security 域），放 core 会形成循环依赖（security→core→security）。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final TokenService tokenService;

    /**
     * 注册（注册即登录）.
     */
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse register(RegisterRequest req) {
        // 用户名唯一校验（逻辑删除自动过滤）
        Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, req.getUsername()));
        if (count != null && count > 0) {
            throw new BizException(ErrorCode.USERNAME_EXISTS);
        }

        SysUser user = new SysUser();
        user.setUsername(req.getUsername());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setRealName(req.getRealName());
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        user.setRole(RoleEnum.STUDENT);
        user.setStatus(UserStatusEnum.ACTIVE);
        user.setMaxBooks(RoleEnum.STUDENT.getDefaultMaxBooks());
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            // 并发注册兜底：selectCount 与 insert 非原子，依赖 DB 唯一约束拦截
            throw new BizException(ErrorCode.USERNAME_EXISTS);
        }

        return issueTokens(user);
    }

    /**
     * 登录.
     */
    public LoginResponse login(LoginRequest req) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, req.getUsername()));
        // 防枚举：用户不存在与密码错误统一返回 BAD_CREDENTIALS
        if (user == null) {
            throw new BizException(ErrorCode.BAD_CREDENTIALS);
        }
        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new BizException(ErrorCode.BAD_CREDENTIALS);
        }
        checkStatus(user);
        return issueTokens(user);
    }

    /**
     * 刷新令牌（轮换，旧 RT 失效）.
     */
    public RefreshResponse refresh(String refreshToken) {
        Claims claims;
        try {
            claims = jwtUtils.parse(refreshToken);
        } catch (ExpiredJwtException e) {
            throw new BizException(ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException e) {
            throw new BizException(ErrorCode.TOKEN_INVALID);
        }
        if (!jwtUtils.isRefresh(claims)) {
            throw new BizException(ErrorCode.TOKEN_INVALID);
        }

        long userId = Long.parseLong(claims.getSubject());
        String oldJti = claims.getId();

        // 生成新 RT 并原子轮换（重放检测）
        JwtUtils.RefreshTokenData newRt = jwtUtils.generateRefreshToken(userId);
        if (tokenService.rotate(userId, oldJti, newRt.jti()) == 1) {
            // 重放：链已撤销，需重新登录
            throw new BizException(ErrorCode.TOKEN_INVALID);
        }

        // 重查用户获取最新 role/status
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        checkStatus(user);

        String accessToken = jwtUtils.generateAccessToken(user.getId(), user.getUsername(), user.getRole().name());
        return RefreshResponse.builder()
                .accessToken(accessToken)
                .refreshToken(newRt.token())
                .expiresIn((int) jwtUtils.getAccessTokenExpiresInSeconds())
                .build();
    }

    /**
     * 登出.
     */
    public void logout(long userId) {
        tokenService.revoke(userId);
    }

    // ==================== 内部方法 ====================

    private LoginResponse issueTokens(SysUser user) {
        String accessToken = jwtUtils.generateAccessToken(user.getId(), user.getUsername(), user.getRole().name());
        JwtUtils.RefreshTokenData rt = jwtUtils.generateRefreshToken(user.getId());
        // 覆盖式存储：旧 RT 立即失效
        tokenService.storeRefresh(user.getId(), rt.jti());
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rt.token())
                .tokenType("Bearer")
                .expiresIn((int) jwtUtils.getAccessTokenExpiresInSeconds())
                .user(toProfile(user))
                .build();
    }

    private void checkStatus(SysUser user) {
        if (user.getStatus() == UserStatusEnum.DISABLED) {
            throw new BizException(ErrorCode.USER_DISABLED);
        }
        if (user.getStatus() == UserStatusEnum.FROZEN) {
            throw new BizException(ErrorCode.ACCOUNT_FROZEN);
        }
    }

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
}
