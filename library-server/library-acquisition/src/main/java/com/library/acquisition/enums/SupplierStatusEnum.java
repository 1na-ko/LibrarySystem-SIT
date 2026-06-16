package com.library.acquisition.enums;

import lombok.Getter;

/**
 * 供应商状态枚举（对齐 supplier.status ENUM）.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Getter
public enum SupplierStatusEnum {
    ACTIVE("活跃"),
    INACTIVE("停用"),
    BLACKLISTED("黑名单");

    private final String description;

    SupplierStatusEnum(String description) { this.description = description; }
}
