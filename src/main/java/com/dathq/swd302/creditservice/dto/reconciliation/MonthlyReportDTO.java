package com.dathq.swd302.creditservice.dto.reconciliation;

import com.dathq.swd302.creditservice.entity.ReconciliationStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// GET /reconciliation/{id}/report   (Screenshot 3: "Report — November 2024")
@Data
@Builder
public class MonthlyReportDTO {
    public String period;                  // "November 2024"
    public LocalDateTime generatedOn;
    public int totalTransactions;
    public int discrepanciesResolved;

    public BigDecimal totalRevenue;
    public BigDecimal totalRefunds;        // shown red "(deducted)"
    public BigDecimal netRevenue;          // = totalRevenue - totalRefunds
    public BigDecimal totalExpenses;       // shown red "(deducted)"
    public BigDecimal netProfit;           // = netRevenue - totalExpenses

    public ReconciliationStatus status;
    public List<ReconciliationSummaryDTO.ExpenseLineDTO> expenseLines;
}
