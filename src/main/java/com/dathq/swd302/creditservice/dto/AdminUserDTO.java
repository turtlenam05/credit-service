package com.dathq.swd302.creditservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record AdminUserDTO(
        UUID userId,
        BigDecimal balance,
        BigDecimal reservedBalance,
        BigDecimal totalSpent,
        String status,
        LocalDateTime createdAt
) {}
