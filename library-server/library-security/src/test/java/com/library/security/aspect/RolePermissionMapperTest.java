package com.library.security.aspect;

import com.library.core.enums.RoleEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RolePermissionMapper 角色权限映射单元测试.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@DisplayName("RolePermissionMapper 角色权限映射")
class RolePermissionMapperTest {

    private final RolePermissionMapper mapper = new RolePermissionMapper();

    @Test
    @DisplayName("ADMIN 应拥有通配权限 *")
    void adminShouldHaveWildcard() {
        assertThat(mapper.permissionsFor(RoleEnum.ADMIN)).contains("*");
        assertThat(mapper.hasPermission(RoleEnum.ADMIN, "anything:x")).isTrue();
    }

    @Test
    @DisplayName("LIBRARIAN 应包含 kg:admin（KG Admin 表达）")
    void librarianShouldContainKgAdmin() {
        assertThat(mapper.permissionsFor(RoleEnum.LIBRARIAN)).contains("kg:admin");
        assertThat(mapper.hasPermission(RoleEnum.LIBRARIAN, "kg:admin")).isTrue();
    }

    @Test
    @DisplayName("STUDENT 应有 book:read 但无 kg:admin")
    void studentPermissions() {
        assertThat(mapper.hasPermission(RoleEnum.STUDENT, "book:read")).isTrue();
        assertThat(mapper.hasPermission(RoleEnum.STUDENT, "kg:admin")).isFalse();
    }

    @Test
    @DisplayName("ACQUISITOR 应有 acquisition:* 系列权限")
    void acquisitorPermissions() {
        assertThat(mapper.hasPermission(RoleEnum.ACQUISITOR, "acquisition:predict")).isTrue();
        assertThat(mapper.hasPermission(RoleEnum.ACQUISITOR, "acquisition:negotiation")).isTrue();
        assertThat(mapper.hasPermission(RoleEnum.ACQUISITOR, "kg:admin")).isFalse();
    }

    @Test
    @DisplayName("TEACHER 与 STUDENT 权限集一致")
    void teacherEqualsStudent() {
        assertThat(mapper.permissionsFor(RoleEnum.TEACHER))
                .isEqualTo(mapper.permissionsFor(RoleEnum.STUDENT));
    }
}
