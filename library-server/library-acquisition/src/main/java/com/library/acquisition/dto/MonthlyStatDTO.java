package com.library.acquisition.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 月度借阅统计 DTO.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyStatDTO {
    private String ym;
    private Long subjectId;
    private Long borrowCount;

    public double getCount() { return borrowCount != null ? borrowCount.doubleValue() : 0.0; }
}
