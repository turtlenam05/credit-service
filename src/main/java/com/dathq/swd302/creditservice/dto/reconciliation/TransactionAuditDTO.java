package com.dathq.swd302.creditservice.dto.reconciliation;

import com.dathq.swd302.creditservice.entity.TransactionMatchStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

// GET /reconciliation/{month}/{year}/audit
// Represents one row in the Transaction Audit table.
@Data
@Builder
public class TransactionAuditDTO {
    public Long auditId;
    public Long transactionId;
    public String txnCode;          // e.g. "TXN-48201"
    public LocalDate date;
    public String description;
    public BigDecimal systemAmount;
    public BigDecimal gatewayAmount; // null → shows "—" in UI
    public TransactionMatchStatus matchStatus;
    public String adminNote;
}
