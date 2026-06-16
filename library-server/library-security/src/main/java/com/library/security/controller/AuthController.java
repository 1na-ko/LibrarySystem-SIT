package com.library.security.controller;

import com.library.common.annotation.NoAuth;
import com.library.common.result.Result;
import com.library.core.dto.LoginRequest;
import com.library.core.dto.RefreshRequest;
import com.library.core.dto.RegisterRequest;
import com.library.core.vo.LoginResponse;
import com.library.core.vo.RefreshResponse;
import com.library.security.context.SecurityUtils;
import com.library.security.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器.
 * <p>
 * 对应 OpenAPI {@code /auth/*} 四端点。login/register/refresh 公开放行（{@link NoAuth}），
 * logout 需认证（从 SecurityContext 取 userId）。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户注册（注册即登录，返回令牌对）.
     */
    @NoAuth
    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success("注册成功", authService.register(request));
    }

    /**
     * 用户登录.
     */
    @NoAuth
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success("登录成功", authService.login(request));
    }

    /**
     * 刷新 Token（轮换，旧 RT 立即失效）.
     */
    @NoAuth
    @PostMapping("/refresh")
    public Result<RefreshResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return Result.success(authService.refresh(request.getRefreshToken()));
    }

    /**
     * 登出（使当前用户的 Refresh Token 失效）.
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout(SecurityUtils.getCurrentUserId());
        return Result.<Void>success("登出成功", null);
    }
}
