package com.dathq.swd302.creditservice.dto;

import java.math.BigDecimal;

public record AdminOverviewDTO(
        long totalWallets,
        long totalTransactions,
        BigDecimal totalRevenueAllTime,
        BigDecimal totalRefundsAllTime,
        BigDecimal netRevenueAllTime,
        long pendingRefundRequests,
        long pendingReconciliations
) {}
