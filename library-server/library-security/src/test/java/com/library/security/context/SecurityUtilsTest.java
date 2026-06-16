package com.library.security.context;

import com.library.common.exception.BizException;
import com.library.core.enums.RoleEnum;
import com.library.core.enums.UserStatusEnum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SecurityUtils 单元测试.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@DisplayName("SecurityUtils 安全上下文工具")
class SecurityUtilsTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("已认证时应返回当前用户与 ID")
    void shouldReturnCurrentUserWhenAuthenticated() {
        LoginUser user = new LoginUser(99L, "bob", RoleEnum.TEACHER,
                UserStatusEnum.ACTIVE, List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));

        assertThat(SecurityUtils.getCurrentUser()).isNotNull();
        assertThat(SecurityUtils.getCurrentUser().getUserId()).isEqualTo(99L);
        assertThat(SecurityUtils.getCurrentUserId()).isEqualTo(99L);
    }

    @Test
    @DisplayName("未认证时 getCurrentUser 返回 null")
    void shouldReturnNullWhenUnauthenticated() {
        SecurityContextHolder.clearContext();
        assertThat(SecurityUtils.getCurrentUser()).isNull();
    }

    @Test
    @DisplayName("未认证时 getCurrentUserId 抛 UNAUTHORIZED")
    void shouldThrowWhenGetCurrentUserIdUnauthenticated() {
        SecurityContextHolder.clearContext();
        assertThatThrownBy(SecurityUtils::getCurrentUserId)
                .isInstanceOf(BizException.class);
    }
}
