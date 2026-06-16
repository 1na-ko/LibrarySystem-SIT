package com.library.acquisition.vo;

import com.library.acquisition.enums.NegotiationStatusEnum;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 谈判记录视图对象.
 * <p>
 * 仅暴露前端需要的字段，隐藏 ORM 内部细节（deleted 标记、strategies/keyTerms/riskWarnings
 * 的 JSON 原始字符串列等），避免实体直接出参造成的内部结构泄漏。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
@Builder
public class NegotiationVO {

    /** 记录 ID */
    private Long id;

    /** 电子资源 ID */
    private Long resourceId;

    /** 供应商 ID */
    private Long supplierId;

    /** 谈判人 ID */
    private Long negotiatorId;

    /** 谈判状态 */
    private NegotiationStatusEnum status;

    /** 底价 */
    private BigDecimal floorPrice;

    /** 最高价 */
    private BigDecimal ceilingPrice;

    /** 建议报价 */
    private BigDecimal suggestedPrice;

    /** 创建时间 */
    private LocalDateTime createTime;
}
