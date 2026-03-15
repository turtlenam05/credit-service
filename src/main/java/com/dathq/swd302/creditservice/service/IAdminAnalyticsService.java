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

    /** Filtered transaction ledger for admin audit and investigation. */
    List<CreditTransaction> getTransactions(AdminTransactionFilter filter);

    /** Top N users ranked by total credits spent (all-time). */
    List<AdminUserDTO> getTopSpenders(int limit);

    /** Aggregate statistics across all user wallets. */
    AdminWalletStatsDTO getWalletStats();
}
