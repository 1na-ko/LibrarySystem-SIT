package com.library.acquisition.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.library.acquisition.enums.NegotiationStatusEnum;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 谈判记录实体（对应 negotiation_record 表）.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
@TableName("negotiation_record")
public class NegotiationRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long resourceId;
    private Long supplierId;
    private Long negotiatorId;
    private BigDecimal floorPrice;
    private BigDecimal ceilingPrice;
    private BigDecimal suggestedPrice;
    /** JSON 列，存谈判策略数组 */
    private String strategies;
    /** JSON 列，存关键条款数组 */
    private String keyTerms;
    /** JSON 列，存风险提示数组 */
    private String riskWarnings;
    private NegotiationStatusEnum status;
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
