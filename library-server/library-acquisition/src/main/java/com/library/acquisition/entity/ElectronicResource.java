package com.library.acquisition.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.library.acquisition.enums.ResourceCategoryEnum;
import com.library.acquisition.enums.ResourceStatusEnum;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 电子资源实体（对应 electronic_resource 表）.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
@TableName("electronic_resource")
public class ElectronicResource {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private ResourceCategoryEnum category;
    private String publisher;
    private BigDecimal annualBudget;
    private Integer userCount;
    private String accessUrl;
    private ResourceStatusEnum status;
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
