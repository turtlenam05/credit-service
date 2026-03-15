package com.dathq.swd302.creditservice.dto;

import java.math.BigDecimal;

public record AdminWalletStatsDTO(
        long totalWallets,
        BigDecimal totalBalanceInSystem,
        BigDecimal totalReservedBalance,
        BigDecimal averageBalance,
        long zeroBalanceWallets
) {}
