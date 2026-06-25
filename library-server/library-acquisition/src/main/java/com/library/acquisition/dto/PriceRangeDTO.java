package com.library.acquisition.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceRangeDTO {
    private BigDecimal floorPrice;
    private BigDecimal ceilingPrice;
    private BigDecimal medianPrice;
    private BigDecimal suggestedOffer;
}
