package com.library.security.aspect;

import com.library.core.enums.RoleEnum;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 角色→权限标识映射.
 * <p>
 * 阶段 1 采用静态派生（无独立权限表）。{@code ADMIN} 拥有通配 {@code *}；
 * KG Admin 通过 {@code LIBRARIAN} + {@code kg:admin} 权限标识表达
 * （架构文档 §2.3 KG Admin 角色说明）。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Component
public class RolePermissionMapper {

    /**
     * 获取角色拥有的权限标识集合.
     *
     * @param role 角色
     * @return 权限标识集合（ADMIN 为 {"*"}）
     */
    public Set<String> permissionsFor(RoleEnum role) {
        return switch (role) {
            case ADMIN -> Set.of("*");
            case LIBRARIAN -> Set.of(
                    "book:read", "book:catalog",
                    "borrow:approve", "borrow:read",
                    "acquisition:duplicate-check",
                    "kg:read", "kg:admin");
            case ACQUISITOR -> Set.of(
                    "book:read",
                    "acquisition:predict", "acquisition:gap",
                    "acquisition:negotiation", "acquisition:duplicate-check");
            case STUDENT, TEACHER -> Set.of(
                    "book:read", "borrow:own", "reservation:own",
                    "kg:read", "recommend:read");
        };
    }

    /**
     * 判断角色是否拥有指定权限.
     *
     * @param role       角色
     * @param permission 权限标识
     * @return 拥有通配或精确匹配时返回 true
     */
    public boolean hasPermission(RoleEnum role, String permission) {
        Set<String> perms = permissionsFor(role);
        return perms.contains("*") || perms.contains(permission);
    }
}
