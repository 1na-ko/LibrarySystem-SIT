package com.library.security.aspect;

import com.library.common.exception.BizException;
import com.library.common.exception.ErrorCode;
import com.library.core.enums.RoleEnum;
import com.library.security.context.LoginUser;
import com.library.security.context.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;

/**
 * RBAC 授权切面.
 * <p>
 * 拦截 {@link RequireRole} / {@link RequirePermission} 注解的方法，校验当前用户角色/权限。
 * 校验失败抛 {@link BizException}({@link ErrorCode#FORBIDDEN})，由全局异常处理器转为 403。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Aspect
@Component
@RequiredArgsConstructor
public class AuthorizationAspect {

    private final RolePermissionMapper rolePermissionMapper;

    /**
     * 角色校验.
     */
    @Around("@annotation(requireRole)")
    public Object checkRole(ProceedingJoinPoint pjp, RequireRole requireRole) throws Throwable {
        LoginUser user = requireAuthenticated();
        validateRole(user.getRole(), requireRole.value(), requireRole.requireAll());
        return pjp.proceed();
    }

    /**
     * 权限校验.
     */
    @Around("@annotation(requirePermission)")
    public Object checkPermission(ProceedingJoinPoint pjp, RequirePermission requirePermission) throws Throwable {
        LoginUser user = requireAuthenticated();
        validatePermission(user.getRole(), requirePermission.value(), requirePermission.requireAll());
        return pjp.proceed();
    }

    /**
     * 角色校验（纯逻辑，便于单元测试）.
     *
     * @param userRole 用户角色
     * @param required 要求的角色数组
     * @param requireAll 是否要求全部满足
     */
    void validateRole(RoleEnum userRole, RoleEnum[] required, boolean requireAll) {
        boolean ok = requireAll
                ? Arrays.stream(required).allMatch(r -> r == userRole)
                : Arrays.stream(required).anyMatch(r -> r == userRole);
        if (!ok) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
    }

    /**
     * 权限校验（纯逻辑，便于单元测试）.
     */
    void validatePermission(RoleEnum userRole, String[] required, boolean requireAll) {
        Set<String> userPerms = rolePermissionMapper.permissionsFor(userRole);
        boolean ok = requireAll
                ? Arrays.stream(required).allMatch(p -> hasPerm(userPerms, p))
                : Arrays.stream(required).anyMatch(p -> hasPerm(userPerms, p));
        if (!ok) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
    }

    private boolean hasPerm(Set<String> userPerms, String permission) {
        return userPerms.contains("*") || userPerms.contains(permission);
    }

    private LoginUser requireAuthenticated() {
        LoginUser user = SecurityUtils.getCurrentUser();
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return user;
    }
}
