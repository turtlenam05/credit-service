package com.dathq.swd302.creditservice.dto.reconciliation;

import lombok.Data;

import java.math.BigDecimal;

// POST /reconciliation/{month}/{year}/reconcile
@Data
public class ReconcileRequestDTO {
    /** Total received by payment gateway (VNĐ). */
    public BigDecimal gatewayReceived;
    /** Optional breakdown by payment method. */
    public BigDecimal bankTransfers;
    public BigDecimal eWallets;
    public BigDecimal otherMethods;
    /** Total platform expenses (VNĐ) — or leave null if entered via /expenses. */
    public BigDecimal totalExpenses;
}
