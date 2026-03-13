package com.dathq.swd302.creditservice.dto.reconciliation;

import com.dathq.swd302.creditservice.entity.ReconciliationStatus;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

// GET /reconciliation/{month}/{year}
// Powers the main dashboard: System Credits card, Payment Gateway card,
// Platform Expenses card, Cross-Check summary.
@Data
public class ReconciliationSummaryDTO {
    public int month;
    public int year;

    // System Credits card
    public BigDecimal totalCreditSold;        // Credits Sold
    public BigDecimal totalCreditUsed;        // Credits Used  (informational)
    public BigDecimal totalRefunded;          // Credits Refunded
    public BigDecimal netSystemCredits;       // = creditSold - refunded  (card header)

    // Payment Gateway card
    public BigDecimal gatewayBankTransfers;   // broken out if available
    public BigDecimal gatewayEWallets;
    public BigDecimal gatewayOther;
    public BigDecimal totalGatewayReceived;   // sum of above

    // Platform Expenses card  (line items from PlatformExpense table)
    public List<ExpenseLineDTO> expenseLines;
    public BigDecimal totalExpenses;

    // Cross-Check summary
    public BigDecimal discrepancy;            // gatewayReceived - creditSold
    public BigDecimal discrepancyPercent;     // |discrepancy| / creditSold * 100
    public boolean withinTolerance;           // discrepancyPercent < 1%

    // Status
    public ReconciliationStatus status;
    public String notes;

    // Audit badge counts for Transaction Audit screen
    public long unmatchedCount;
    public long partialCount;

    @Data @Builder
    public static class ExpenseLineDTO {
        public String category;
        public BigDecimal amount;
        public String description;
    }
}
