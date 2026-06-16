package com.library.acquisition.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.library.acquisition.enums.ResourceCategoryEnum;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 成交记录实体（对应 deal_record 表）.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
@TableName("deal_record")
public class DealRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long supplierId;
    private String resourceName;
    private ResourceCategoryEnum category;
    private BigDecimal dealPrice;
    private LocalDate dealDate;
    private String contractPeriod;
    private String notes;
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
