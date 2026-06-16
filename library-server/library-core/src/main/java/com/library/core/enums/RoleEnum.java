package com.library.core.enums;

import lombok.Getter;

/**
 * 用户角色枚举.
 * <p>
 * 对应 {@code sys_user.role} 列（MySQL ENUM）。
 * MyBatis-Plus {@code MybatisEnumTypeHandler} 在枚举无 {@code @EnumValue} 字段时，
 * 默认按枚举 {@link #name()} 与数据库枚举值互转，无需额外配置。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Getter
public enum RoleEnum {

    /** 学生（默认可借 5 册） */
    STUDENT("学生", 5),
    /** 教师（默认可借 15 册） */
    TEACHER("教师", 15),
    /** 图书管理员（默认可借 10 册） */
    LIBRARIAN("图书管理员", 10),
    /** 采编员（默认可借 10 册） */
    ACQUISITOR("采编员", 10),
    /** 系统管理员（默认可借 15 册） */
    ADMIN("系统管理员", 15);

    /** 角色描述（仅展示用，不落库） */
    private final String description;
    /** 默认最大可借数量 */
    private final int defaultMaxBooks;

    RoleEnum(String description, int defaultMaxBooks) {
        this.description = description;
        this.defaultMaxBooks = defaultMaxBooks;
    }
}
