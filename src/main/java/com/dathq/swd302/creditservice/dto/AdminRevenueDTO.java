package com.dathq.swd302.creditservice.dto;

import java.math.BigDecimal;

public record AdminRevenueDTO(
        int month,
        int year,
        BigDecimal totalRevenue,
        BigDecimal totalRefunds,
        BigDecimal netRevenue,
        int transactionCount,
        BigDecimal aiChatDeductions,
        BigDecimal postCharges,
        BigDecimal listingFees,
        BigDecimal prevMonthRevenue,
        double revenueGrowthPercent
) {}
