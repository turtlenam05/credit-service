package com.dathq.swd302.creditservice.controller;


import com.dathq.swd302.creditservice.dto.reconciliation.*;
import com.dathq.swd302.creditservice.entity.MonthlyReconciliation;
import com.dathq.swd302.creditservice.entity.PlatformExpense;
import com.dathq.swd302.creditservice.security.JwtClaims;
import com.dathq.swd302.creditservice.security.JwtUser;
import com.dathq.swd302.creditservice.service.IReconciliationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reconciliation")
@RequiredArgsConstructor
public class ReconciliationController {
    private final IReconciliationService reconciliationService;
    private final String ROLE_ADMIN = "ADMIN";

    @GetMapping("/{month}/{year}")
    public ResponseEntity<ReconciliationSummaryDTO> getSummary(
            @PathVariable int month,
            @PathVariable int year) {
        return ResponseEntity.ok(reconciliationService.getSummary(month, year));
    }

    @PostMapping("/{month}/{year}/reconcile")
    public ResponseEntity<MonthlyReconciliation> reconcile(
            @PathVariable int month,
            @PathVariable int year,
            @RequestBody ReconcileRequestDTO request,
            @JwtUser JwtClaims claims) {

        if (!ROLE_ADMIN.equals(claims.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(reconciliationService.reconcile(month, year, request, claims.getUserId()));
    }

    @GetMapping("/history")
    public ResponseEntity<List<MonthlyReconciliation>> getHistory() {
        return ResponseEntity.ok(reconciliationService.getHistory());
    }

    @PostMapping("/{month}/{year}/audit/match")
    public ResponseEntity<AuditSummaryDTO> submitGatewayRecords(
            @PathVariable int month,
            @PathVariable int year,
            @RequestBody List<GatewayRecordDTO> records,
            @JwtUser JwtClaims claims) {

        if (!ROLE_ADMIN.equals(claims.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(reconciliationService.submitGatewayRecords(month, year, records));
    }

    @GetMapping("/{month}/{year}/audit")
    public ResponseEntity<AuditSummaryDTO> getAudit(
            @PathVariable int month,
            @PathVariable int year) {
        return ResponseEntity.ok(reconciliationService.getAudit(month, year));
    }

    @PostMapping("/adjust")
    public ResponseEntity<Void> adjust(
            @RequestBody ManualAdjustmentDTO dto,
            @JwtUser JwtClaims claims) {

        if (!ROLE_ADMIN.equals(claims.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        reconciliationService.applyManualAdjustment(dto, claims.getUserId());
        return ResponseEntity.ok().build();
    }


    @PostMapping("/{id}/resolve")
    public ResponseEntity<MonthlyReconciliation> resolve(
            @PathVariable Long id,
            @RequestBody ResolveDiscrepancyDTO dto,
            @JwtUser JwtClaims claims) {

        if (!ROLE_ADMIN.equals(claims.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(reconciliationService.resolveDiscrepancy(id, dto, claims.getUserId()));
    }

    @GetMapping("/{id}/report")
    public ResponseEntity<MonthlyReportDTO> getReport(@PathVariable Long id) {
        return ResponseEntity.ok(reconciliationService.generateReport(id));
    }

    @PostMapping("/expenses")
    public ResponseEntity<PlatformExpense> addExpense(
            @RequestBody ExpenseCreateDTO dto,
            @JwtUser JwtClaims claims) {

        if (!ROLE_ADMIN.equals(claims.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(reconciliationService.addExpense(dto, claims.getUserId()));
    }

    @GetMapping("/{month}/{year}/expenses")
    public ResponseEntity<List<PlatformExpense>> getExpenses(
            @PathVariable int month,
            @PathVariable int year) {
        return ResponseEntity.ok(reconciliationService.getExpenses(month, year));
    }
}
