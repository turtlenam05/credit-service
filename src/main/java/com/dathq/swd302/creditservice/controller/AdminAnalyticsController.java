package com.dathq.swd302.creditservice.controller;

import com.dathq.swd302.creditservice.dto.*;
import com.dathq.swd302.creditservice.entity.CreditTransaction;
import com.dathq.swd302.creditservice.security.JwtClaims;
import com.dathq.swd302.creditservice.security.JwtUser;
import com.dathq.swd302.creditservice.service.IAdminAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

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
     * Full transaction ledger for a given month/year. Useful for auditing
     * or exporting raw data.
     */
    @GetMapping("/transactions")
    public ResponseEntity<List<CreditTransaction>> getTransactions(
            @JwtUser JwtClaims claims,
            @RequestParam int month,
            @RequestParam int year) {
        requireAdmin(claims);
        return ResponseEntity.ok(adminAnalyticsService.getTransactionsByMonth(month, year));
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
