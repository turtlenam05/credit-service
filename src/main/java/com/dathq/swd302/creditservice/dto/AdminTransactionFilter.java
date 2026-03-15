package com.dathq.swd302.creditservice.dto;

import com.dathq.swd302.creditservice.entity.TransactionStatus;
import com.dathq.swd302.creditservice.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AdminTransactionFilter(
        Integer month,
        Integer year,
        List<UUID> userIds,
        TransactionType type,
        TransactionStatus status,
        String referenceType,
        String referenceId,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        LocalDateTime fromDate,
        LocalDateTime toDate,
        String sortBy,
        String sortDir
) {}
