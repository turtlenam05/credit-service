package com.dathq.swd302.creditservice.dto;

import java.math.BigDecimal;

public record AdminCreditUsageDTO(
        int month,
        int year,
        BigDecimal totalPurchases,
        BigDecimal totalAiChatDeductions,
        BigDecimal totalPostCharges,
        BigDecimal totalListingFees,
        BigDecimal totalRefunds,
        long purchaseCount,
        long aiChatCount,
        long postChargeCount,
        long listingFeeCount,
        long refundCount
) {}
