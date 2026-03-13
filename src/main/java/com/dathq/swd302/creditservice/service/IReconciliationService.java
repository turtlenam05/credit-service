package com.dathq.swd302.creditservice.service;

import com.dathq.swd302.creditservice.dto.reconciliation.*;
import com.dathq.swd302.creditservice.entity.MonthlyReconciliation;
import com.dathq.swd302.creditservice.entity.PlatformExpense;

import java.util.List;
import java.util.UUID;

public interface IReconciliationService {
    ReconciliationSummaryDTO getSummary(int month, int year);
    MonthlyReconciliation reconcile(int month, int year, ReconcileRequestDTO request, UUID adminId);
    AuditSummaryDTO submitGatewayRecords(int month, int year, List<GatewayRecordDTO> gatewayRecords);
    AuditSummaryDTO getAudit(int month, int year);
    void applyManualAdjustment(ManualAdjustmentDTO dto, UUID adminId);
    MonthlyReconciliation resolveDiscrepancy(Long reconciliationId, ResolveDiscrepancyDTO dto, UUID adminId);
    MonthlyReportDTO generateReport(Long reconciliationId);
    PlatformExpense addExpense(ExpenseCreateDTO dto, UUID adminId);
    List<PlatformExpense> getExpenses(int month, int year);
    List<MonthlyReconciliation> getHistory();
}
