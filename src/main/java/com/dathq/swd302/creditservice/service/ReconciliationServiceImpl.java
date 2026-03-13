package com.dathq.swd302.creditservice.service;

import com.dathq.swd302.creditservice.dto.reconciliation.*;
import com.dathq.swd302.creditservice.entity.*;
import com.dathq.swd302.creditservice.exception.InsufficientBalanceException;
import com.dathq.swd302.creditservice.exception.WalletNotFoundException;
import com.dathq.swd302.creditservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReconciliationServiceImpl implements IReconciliationService{

    private static final BigDecimal DISCREPANCY_THRESHOLD = new BigDecimal("0.01"); // 1 %
    private final ReconciliationRepository reconciliationRepo;
    private final PlatformExpenseRepository expenseRepo;
    private final TransactionRepository transactionRepo;
    private final TransactionAuditRepository auditRepo;
    private final UserWalletRepository walletRepo;
    private final IKafkaProducerService kafkaProducerService;

    // Summary
    @Override
    public ReconciliationSummaryDTO getSummary(int month, int year) {
        BigDecimal creditSold = safe(transactionRepo.getRevenue(month, year));
        BigDecimal refunded   = safe(transactionRepo.getRefunds(month, year));
        BigDecimal expenses   = safe(expenseRepo.sumByMonthAndYear(month, year));

        MonthlyReconciliation record =
                reconciliationRepo.findByMonthAndYear(month, year).orElse(null);

        BigDecimal gatewayReceived = record != null ? record.getTotalGatewayReceived() : BigDecimal.ZERO;
        // Fix 1: compare gateway against net system credits (sold - refunded),
        // not raw creditSold — refunds reduce the actual money owed by the gateway.
        BigDecimal systemNet       = creditSold.subtract(refunded);
        BigDecimal discrepancy     = gatewayReceived.subtract(systemNet);
        BigDecimal discrepancyPct  = systemNet.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : discrepancy.abs().divide(systemNet, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        List<ReconciliationSummaryDTO.ExpenseLineDTO> expenseLines = expenseRepo
                .findByMonthAndYear(month, year).stream()
                .map(e -> ReconciliationSummaryDTO.ExpenseLineDTO.builder()
                        .category(e.getCategory())
                        .amount(e.getAmount())
                        .description(e.getDescription())
                        .build())
                .collect(Collectors.toList());

        // Badge counts for Transaction Audit screen
        long unmatchedCount = 0;
        long partialCount   = 0;
        if (record != null) {
            unmatchedCount = auditRepo.countByReconciliation_IdAndMatchStatus(record.getId(), TransactionMatchStatus.UNMATCHED);
            partialCount   = auditRepo.countByReconciliation_IdAndMatchStatus(record.getId(), TransactionMatchStatus.PARTIAL);
        }

        ReconciliationSummaryDTO dto = new ReconciliationSummaryDTO();
        dto.month                = month;
        dto.year                 = year;
        dto.totalCreditSold      = creditSold;
        dto.totalRefunded        = refunded;
        dto.netSystemCredits     = creditSold.subtract(refunded);
        dto.totalGatewayReceived = gatewayReceived;
        dto.totalExpenses        = expenses;
        dto.expenseLines         = expenseLines;
        dto.discrepancy          = discrepancy;
        dto.discrepancyPercent   = discrepancyPct;
        dto.withinTolerance      = discrepancyPct.compareTo(new BigDecimal("1")) < 0;
        dto.status               = record != null ? record.getStatus() : ReconciliationStatus.PENDING;
        dto.notes                = record != null ? record.getNotes() : null;
        dto.unmatchedCount       = unmatchedCount;
        dto.partialCount         = partialCount;
        return dto;
    }

//    Run reconciliation
    @Override
    @Transactional
    public MonthlyReconciliation reconcile(int month, int year, ReconcileRequestDTO request, UUID adminId) {
        BigDecimal creditSold = safe(transactionRepo.getRevenue(month, year));
        BigDecimal refunded   = safe(transactionRepo.getRefunds(month, year));
        BigDecimal expenses   = request.totalExpenses != null
                ? request.totalExpenses
                : safe(expenseRepo.sumByMonthAndYear(month, year));
        BigDecimal received   = safe(request.gatewayReceived);

        // Fix 2: compare gateway against net (sold - refunded), same logic as getSummary()
        BigDecimal systemNet      = creditSold.subtract(refunded);
        BigDecimal discrepancy    = received.subtract(systemNet);
        BigDecimal discrepancyPct = systemNet.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : discrepancy.abs().divide(systemNet, 6, RoundingMode.HALF_UP);

        ReconciliationStatus status = discrepancyPct.compareTo(DISCREPANCY_THRESHOLD) < 0
                ? ReconciliationStatus.RECONCILED
                : ReconciliationStatus.DISCREPANCY;

        BigDecimal netProfit = creditSold.subtract(refunded).subtract(expenses);

        MonthlyReconciliation record = reconciliationRepo
                .findByMonthAndYear(month, year)
                .orElse(MonthlyReconciliation.builder().month(month).year(year).build());

        record.setTotalCreditSold(creditSold);
        record.setTotalRefunded(refunded);
        record.setTotalGatewayReceived(received);
        record.setTotalExpenses(expenses);
        record.setDiscrepancy(discrepancy);
        record.setNetProfit(netProfit);
        record.setStatus(status);

        MonthlyReconciliation saved = reconciliationRepo.save(record);
        log.info("Reconciliation {}/{} → status={} discrepancy={}", month, year, status, discrepancy);
        return saved;
    }

    /** Transaction Audit (matching)
     * Admin submits gateway records for matching against system transactions.
     * Creates / updates TransactionAuditEntry rows and returns the full audit table.
     *
     * Matching logic:
     *  - gatewayAmount == null                    → UNMATCHED
     *  - gatewayAmount == systemAmount            → MATCHED
     *  - gatewayAmount != null && != systemAmount → PARTIAL
     */
    @Override
    @Transactional
    public AuditSummaryDTO submitGatewayRecords(int month, int year, List<GatewayRecordDTO> gatewayRecords) {
        MonthlyReconciliation record = reconciliationRepo
                .findByMonthAndYear(month, year)
                .orElseThrow(() -> new RuntimeException(
                        "No reconciliation record for " + month + "/" + year
                                + ". Call /reconcile first."));

        // Build a lookup: referenceId → gateway record
        java.util.Map<String, GatewayRecordDTO> gwMap = gatewayRecords.stream()
                .collect(Collectors.toMap(g -> g.transactionReferenceId, g -> g));

        List<CreditTransaction> systemTxns = transactionRepo.findPurchasesByMonth(month, year);

        for (CreditTransaction txn : systemTxns) {
            GatewayRecordDTO gw = gwMap.get(txn.getReferenceId());

            TransactionMatchStatus matchStatus;
            BigDecimal gatewayAmount = null;
            if (gw == null || gw.gatewayAmount == null) {
                matchStatus = TransactionMatchStatus.UNMATCHED;
            } else {
                gatewayAmount = gw.gatewayAmount;
                matchStatus = txn.getAmount().compareTo(gatewayAmount) == 0
                        ? TransactionMatchStatus.MATCHED
                        : TransactionMatchStatus.PARTIAL;
            }

            // Fix 3: upsert by (reconciliationId + transactionId) — matchStatus can change
            // (UNMATCHED → MATCHED after re-submission), so it must not be part of the lookup key.
            TransactionAuditEntry entry = auditRepo
                    .findByReconciliation_IdAndTransaction_TransactionId(
                            record.getId(), txn.getTransactionId())
                    .orElse(TransactionAuditEntry.builder()
                            .reconciliation(record)
                            .transaction(txn)
                            .build());

            entry.setDescription(gw != null && gw.description != null
                    ? gw.description
                    : txn.getNotes());
            entry.setSystemAmount(txn.getAmount());
            entry.setGatewayAmount(gatewayAmount);
            entry.setMatchStatus(matchStatus);
            auditRepo.save(entry);
        }

        return buildAuditSummary(record.getId());
    }

    @Override
    public AuditSummaryDTO getAudit(int month, int year) {
        MonthlyReconciliation record = reconciliationRepo
                .findByMonthAndYear(month, year)
                .orElseThrow(() -> new RuntimeException("No reconciliation record found."));
        return buildAuditSummary(record.getId());
    }

    //    Manual adjustment
    @Override
    @Transactional
    public void applyManualAdjustment(ManualAdjustmentDTO dto, UUID adminId) {
        UserWallet wallet = walletRepo.findByUserId(dto.userId)
                .orElseThrow(() -> new WalletNotFoundException(dto.userId));

        BigDecimal amount = BigDecimal.valueOf(dto.creditAmount);

        if (amount.compareTo(BigDecimal.ZERO) < 0
                && wallet.getBalance().compareTo(amount.abs()) < 0) {
            throw new InsufficientBalanceException(
                    dto.userId, dto.creditAmount, wallet.getBalance().doubleValue());
        }

        wallet.setBalance(wallet.getBalance().add(amount));
        wallet.setUpdatedAt(LocalDateTime.now());

        CreditTransaction tx = CreditTransaction.builder()
                .wallet(wallet)
                .amount(amount)
                .type(TransactionType.REFUND)
                .referenceType("MANUAL_ADJUSTMENT")
                .referenceId(dto.referenceId)
                .status(TransactionStatus.SUCCESS)
                .notes("Manual adjustment by admin " + adminId + ": " + dto.reason)
                .createdAt(LocalDateTime.now())
                .build();

        transactionRepo.save(tx);
        walletRepo.save(wallet);

        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            kafkaProducerService.publishCreditRefunded(dto.userId, dto.creditAmount, "MANUAL_ADJUSTMENT");
        } else {
            kafkaProducerService.publishCreditDeducted(dto.userId, Math.abs(dto.creditAmount), "MANUAL_ADJUSTMENT");
        }

        log.info("Manual adjustment: userId={} amount={} reason={}", dto.userId, amount, dto.reason);
    }

    //  Resolve discrepancy
    @Override
    @Transactional
    public MonthlyReconciliation resolveDiscrepancy(Long reconciliationId, ResolveDiscrepancyDTO dto, UUID adminId) {
        MonthlyReconciliation record = reconciliationRepo.findById(reconciliationId)
                .orElseThrow(() -> new RuntimeException("Reconciliation not found: " + reconciliationId));

        if (record.getStatus() != ReconciliationStatus.DISCREPANCY) {
            throw new RuntimeException("Only DISCREPANCY records can be resolved. Status: " + record.getStatus());
        }

        record.setStatus(ReconciliationStatus.RESOLVED);
        record.setNotes(dto.notes);
        return reconciliationRepo.save(record);
    }

    // Monthly report
    @Override
    public MonthlyReportDTO generateReport(Long reconciliationId) {
        MonthlyReconciliation record = reconciliationRepo.findById(reconciliationId)
                .orElseThrow(() -> new RuntimeException("Reconciliation not found: " + reconciliationId));

        // Fix 5: use COUNT query instead of loading all rows just to call .size()
        int totalTxns = transactionRepo.countByMonthAndYear(record.getMonth(), record.getYear());
        long resolved = auditRepo
                .findByReconciliation_IdAndMatchStatus(reconciliationId, TransactionMatchStatus.MATCHED)
                .size(); // "8 discrepancies resolved" = entries that had issues and were fixed

        List<ReconciliationSummaryDTO.ExpenseLineDTO> expenseLines = expenseRepo
                .findByMonthAndYear(record.getMonth(), record.getYear())
                .stream()
                .map(e -> ReconciliationSummaryDTO.ExpenseLineDTO.builder()
                        .category(e.getCategory())
                        .amount(e.getAmount())
                        .description(e.getDescription())
                        .build())
                .collect(Collectors.toList());

        BigDecimal netRevenue = record.getTotalCreditSold().subtract(record.getTotalRefunded());

        return MonthlyReportDTO.builder()
                .period(monthName(record.getMonth()) + " " + record.getYear())
                .generatedOn(LocalDateTime.now())
                .totalTransactions(totalTxns)
                .discrepanciesResolved((int) resolved)
                .totalRevenue(record.getTotalCreditSold())
                .totalRefunds(record.getTotalRefunded())
                .netRevenue(netRevenue)
                .totalExpenses(record.getTotalExpenses())
                .netProfit(record.getNetProfit())
                .status(record.getStatus())
                .expenseLines(expenseLines)
                .build();
    }

    // Expenses
    @Override
    @Transactional
    public PlatformExpense addExpense(ExpenseCreateDTO dto, UUID adminId) {
        return expenseRepo.save(PlatformExpense.builder()
                .month(dto.month).year(dto.year)
                .category(dto.category)
                .amount(dto.amount)
                .description(dto.description)
                .createdBy(adminId)
                .build());
    }

    @Override
    public List<PlatformExpense> getExpenses(int month, int year) {
        return expenseRepo.findByMonthAndYear(month, year);
    }

    @Override
    public List<MonthlyReconciliation> getHistory() {
        return reconciliationRepo.findAllByOrderByYearDescMonthDesc();
    }

    // Private Helper method
    private BigDecimal safe(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private AuditSummaryDTO buildAuditSummary(Long reconciliationId) {
        List<TransactionAuditEntry> entries = auditRepo.findByReconciliation_IdOrderByCreatedAtDesc(reconciliationId);

        List<TransactionAuditDTO> dtos = entries.stream()
                .map(e -> TransactionAuditDTO.builder()
                        .auditId(e.getId())
                        .transactionId(e.getTransaction().getTransactionId())
                        .txnCode("TXN-" + e.getTransaction().getTransactionId())
                        .date(e.getCreatedAt() != null ? e.getCreatedAt().toLocalDate() : null)
                        .description(e.getDescription())
                        .systemAmount(e.getSystemAmount())
                        .gatewayAmount(e.getGatewayAmount())
                        .matchStatus(e.getMatchStatus())
                        .adminNote(e.getAdminNote())
                        .build())
                .collect(Collectors.toList());

        // single pass over entries for all three counts — avoids iterating the list 3 time
        long matchedCount = 0, unmatchedCount = 0, partialCount = 0;
        for (TransactionAuditEntry e : entries) {
            switch (e.getMatchStatus()) {
                case MATCHED   -> matchedCount++;
                case UNMATCHED -> unmatchedCount++;
                case PARTIAL   -> partialCount++;
            }
        }

        return AuditSummaryDTO.builder()
                .totalCount(entries.size())
                .matchedCount(matchedCount)
                .unmatchedCount(unmatchedCount)
                .partialCount(partialCount)
                .entries(dtos)
                .build();
    }

    private String monthName(int month) {
        return new java.time.format.DateTimeFormatterBuilder()
                .appendPattern("MMMM")
                .toFormatter()
                .format(java.time.Month.of(month));
    }
}
