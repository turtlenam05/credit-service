package com.dathq.swd302.creditservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MonthlyReportDTO {
    private String month;
    private BigDecimal revenue;
    private BigDecimal refunds;
    private BigDecimal expenses;
    private BigDecimal profit;
}
