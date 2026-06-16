package com.library.security.aspect;

import com.library.common.exception.BizException;
import com.library.core.enums.RoleEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AuthorizationAspect RBAC 切面单元测试.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@DisplayName("AuthorizationAspect RBAC 授权")
class AuthorizationAspectTest {

    private AuthorizationAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new AuthorizationAspect(new RolePermissionMapper());
    }

    @Nested
    @DisplayName("角色校验 validateRole")
    class RoleValidation {

        @Test
        @DisplayName("用户角色在要求列表中应通过（OR 语义）")
        void shouldPassWhenRoleInList() {
            aspect.validateRole(RoleEnum.LIBRARIAN,
                    new RoleEnum[]{RoleEnum.LIBRARIAN, RoleEnum.ADMIN}, false);
        }

        @Test
        @DisplayName("用户角色不在要求列表中应抛 FORBIDDEN")
        void shouldThrowWhenRoleNotInList() {
            assertThatThrownBy(() -> aspect.validateRole(RoleEnum.STUDENT,
                    new RoleEnum[]{RoleEnum.LIBRARIAN, RoleEnum.ADMIN}, false))
                    .isInstanceOf(BizException.class);
        }

        @Test
        @DisplayName("requireAll=true 时用户须满足全部角色")
        void shouldRequireAllWhenFlagTrue() {
            // 单角色用户无法满足 requireAll 的多角色要求
            assertThatThrownBy(() -> aspect.validateRole(RoleEnum.ADMIN,
                    new RoleEnum[]{RoleEnum.ADMIN, RoleEnum.LIBRARIAN}, true))
                    .isInstanceOf(BizException.class);
        }

        @Test
        @DisplayName("requireAll=true 时单角色要求应通过")
        void shouldPassWhenRequireAllSingleRole() {
            aspect.validateRole(RoleEnum.ADMIN, new RoleEnum[]{RoleEnum.ADMIN}, true);
        }
    }

    @Nested
    @DisplayName("权限校验 validatePermission")
    class PermissionValidation {

        @Test
        @DisplayName("LIBRARIAN 应拥有 kg:admin 权限（KG Admin 表达）")
        void librarianShouldHaveKgAdmin() {
            aspect.validatePermission(RoleEnum.LIBRARIAN, new String[]{"kg:admin"}, false);
        }

        @Test
        @DisplayName("STUDENT 不应拥有 kg:admin 权限")
        void studentShouldNotHaveKgAdmin() {
            assertThatThrownBy(() -> aspect.validatePermission(RoleEnum.STUDENT,
                    new String[]{"kg:admin"}, false))
                    .isInstanceOf(BizException.class);
        }

        @Test
        @DisplayName("ADMIN 通配应拥有任意权限")
        void adminShouldHaveAnyPermission() {
            assertThatCode(() -> aspect.validatePermission(RoleEnum.ADMIN,
                    new String[]{"any:permission", "whatever:x"}, false))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("ACQUISITOR 应拥有 acquisition:predict")
        void acquisitorShouldHavePredict() {
            aspect.validatePermission(RoleEnum.ACQUISITOR, new String[]{"acquisition:predict"}, false);
        }

        @Test
        @DisplayName("requireAll=true 时须全部满足")
        void shouldRequireAllPermissions() {
            // STUDENT 有 book:read 但无 kg:admin → requireAll 失败
            assertThatThrownBy(() -> aspect.validatePermission(RoleEnum.STUDENT,
                    new String[]{"book:read", "kg:admin"}, true))
                    .isInstanceOf(BizException.class);
        }
    }
}
