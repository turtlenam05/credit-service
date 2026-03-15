package com.dathq.swd302.creditservice.service;

import com.dathq.swd302.creditservice.dto.*;
import com.dathq.swd302.creditservice.entity.CreditTransaction;

import java.util.List;

public interface IAdminAnalyticsService {

    /** High-level platform snapshot: wallet counts, all-time revenue, pending queues. */
    AdminOverviewDTO getOverview();

    /** Revenue breakdown for a specific month/year with month-over-month comparison. */
    AdminRevenueDTO getRevenueStats(int month, int year);

    /** Credit consumption breakdown by transaction type for a specific month/year. */
    AdminCreditUsageDTO getCreditUsage(int month, int year);

    /** All transactions recorded in a given month/year (detailed ledger view). */
    List<CreditTransaction> getTransactionsByMonth(int month, int year);

    /** Top N users ranked by total credits spent (all-time). */
    List<AdminUserDTO> getTopSpenders(int limit);

    /** Aggregate statistics across all user wallets. */
    AdminWalletStatsDTO getWalletStats();
}
