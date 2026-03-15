package com.dathq.swd302.creditservice.controller;

import com.dathq.swd302.creditservice.dto.*;
import com.dathq.swd302.creditservice.entity.CreditTransaction;
import com.dathq.swd302.creditservice.entity.TransactionStatus;
import com.dathq.swd302.creditservice.entity.TransactionType;
import com.dathq.swd302.creditservice.security.JwtClaims;
import com.dathq.swd302.creditservice.security.JwtUser;
import com.dathq.swd302.creditservice.service.IAdminAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/analytics")
@RequiredArgsConstructor
public class AdminAnalyticsController {

    private final IAdminAnalyticsService adminAnalyticsService;

    // ─── Platform overview ─────────────────────────────────────────────────────

    /**
     * Returns a high-level platform snapshot: total wallets, all-time revenue,
     * total transactions, and counts for queues that need attention.
     */
    @GetMapping("/overview")
    public ResponseEntity<AdminOverviewDTO> getOverview(@JwtUser JwtClaims claims) {
        requireAdmin(claims);
        return ResponseEntity.ok(adminAnalyticsService.getOverview());
    }

    // ─── Revenue ───────────────────────────────────────────────────────────────

    /**
     * Monthly revenue breakdown with a month-over-month growth comparison.
     */
    @GetMapping("/revenue")
    public ResponseEntity<AdminRevenueDTO> getRevenueStats(
            @JwtUser JwtClaims claims,
            @RequestParam int month,
            @RequestParam int year) {
        requireAdmin(claims);
        return ResponseEntity.ok(adminAnalyticsService.getRevenueStats(month, year));
    }

    // ─── Credit usage ──────────────────────────────────────────────────────────

    /**
     * Credit consumption breakdown by transaction type (purchases, AI chat,
     * post charges, listing fees, refunds) for a given month/year.
     */
    @GetMapping("/credit-usage")
    public ResponseEntity<AdminCreditUsageDTO> getCreditUsage(
            @JwtUser JwtClaims claims,
            @RequestParam int month,
            @RequestParam int year) {
        requireAdmin(claims);
        return ResponseEntity.ok(adminAnalyticsService.getCreditUsage(month, year));
    }

    // ─── Transactions ledger ───────────────────────────────────────────────────

    /**
     * Filterable transaction ledger for auditing and investigation.
     * Supports filtering by month/year, users, type, status, reference fields,
     * amount range, date range, and sorting by amount/date/status/type/user.
     */
    @GetMapping("/transactions")
    public ResponseEntity<List<CreditTransaction>> getTransactions(
            @JwtUser JwtClaims claims,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) List<UUID> userIds,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) String referenceType,
            @RequestParam(required = false) String referenceId,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        requireAdmin(claims);
        AdminTransactionFilter filter = new AdminTransactionFilter(
                month,
                year,
                userIds,
                type,
                status,
                referenceType,
                referenceId,
                minAmount,
                maxAmount,
                fromDate,
                toDate,
                sortBy,
                sortDir
        );
        return ResponseEntity.ok(adminAnalyticsService.getTransactions(filter));
    }

    // ─── User analytics ────────────────────────────────────────────────────────

    /**
     * Top spenders ranked by total credits spent (all-time).
     * {@code limit} defaults to 10; capped at 100.
     */
    @GetMapping("/users/top-spenders")
    public ResponseEntity<List<AdminUserDTO>> getTopSpenders(
            @JwtUser JwtClaims claims,
            @RequestParam(defaultValue = "10") int limit) {
        requireAdmin(claims);
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        return ResponseEntity.ok(adminAnalyticsService.getTopSpenders(safeLimit));
    }

    // ─── Wallet statistics ─────────────────────────────────────────────────────

    /**
     * Aggregate wallet statistics: total balance in the system, reserved balance,
     * average balance per user, and the count of zero-balance wallets.
     */
    @GetMapping("/wallets")
    public ResponseEntity<AdminWalletStatsDTO> getWalletStats(@JwtUser JwtClaims claims) {
        requireAdmin(claims);
        return ResponseEntity.ok(adminAnalyticsService.getWalletStats());
    }

    // ─── Guard ─────────────────────────────────────────────────────────────────

    private void requireAdmin(JwtClaims claims) {
        if (claims == null || !"ADMIN".equalsIgnoreCase(claims.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
    }
}
