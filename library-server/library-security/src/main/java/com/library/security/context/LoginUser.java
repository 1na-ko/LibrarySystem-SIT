package com.library.security.context;

import com.library.core.enums.RoleEnum;
import com.library.core.enums.UserStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * 已认证用户主体（实现 Spring Security {@link UserDetails}）.
 * <p>
 * 由 {@code JwtAuthenticationFilter} 在验签通过后从 JWT claims 构造，存入 SecurityContext。
 * authorities 以 {@code ROLE_} + 角色名挂载，供 {@code @PreAuthorize} 与自定义切面使用。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public class LoginUser implements UserDetails {

    /** 用户 ID */
    private final long userId;
    /** 用户名 */
    private final String username;
    /** 角色 */
    private final RoleEnum role;
    /** 账户状态（JWT 过滤器侧默认 ACTIVE，状态变更在 refresh/login 时校验） */
    private final UserStatusEnum status;
    /** 权限集合 */
    private final Collection<? extends GrantedAuthority> authorities;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        // JWT 无状态认证场景下不需要密码
        return null;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != UserStatusEnum.DISABLED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatusEnum.ACTIVE;
    }
}
