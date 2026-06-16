package com.library.acquisition.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.library.acquisition.enums.SupplierStatusEnum;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 供应商实体（对应 supplier 表）.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
@TableName("supplier")
public class Supplier {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String contactPerson;
    private String contactEmail;
    private String contactPhone;
    private Integer partnershipYears;
    private BigDecimal reliability;
    private BigDecimal marketShare;
    private SupplierStatusEnum status;
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
