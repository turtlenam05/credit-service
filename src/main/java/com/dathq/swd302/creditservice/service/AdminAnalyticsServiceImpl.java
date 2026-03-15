package com.dathq.swd302.creditservice.service;

import com.dathq.swd302.creditservice.dto.*;
import com.dathq.swd302.creditservice.entity.*;
import com.dathq.swd302.creditservice.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminAnalyticsServiceImpl implements IAdminAnalyticsService {

    private final UserWalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final RefundRequestRepository refundRequestRepository;
    private final ReconciliationRepository reconciliationRepository;

    @Override
    public AdminOverviewDTO getOverview() {
        long totalWallets = walletRepository.count();
        long totalTransactions = transactionRepository.count();

        BigDecimal totalRevenue = transactionRepository.sumAmountByType(TransactionType.PURCHASE);
        BigDecimal totalRefunds = transactionRepository.sumAmountByType(TransactionType.REFUND);
        BigDecimal netRevenue = totalRevenue.subtract(totalRefunds);

        long pendingRefunds = refundRequestRepository.findByStatus(RefundStatus.PENDING).size();
        long pendingReconciliations = reconciliationRepository.findByStatus(ReconciliationStatus.PENDING).size();

        return new AdminOverviewDTO(
                totalWallets,
                totalTransactions,
                totalRevenue,
                totalRefunds,
                netRevenue,
                pendingRefunds,
                pendingReconciliations
        );
    }

    @Override
    public AdminRevenueDTO getRevenueStats(int month, int year) {
        BigDecimal revenue = coalesce(transactionRepository.getRevenue(month, year));
        BigDecimal refunds = coalesce(transactionRepository.getRefunds(month, year));
        BigDecimal netRevenue = revenue.subtract(refunds);
        int txCount = transactionRepository.countByMonthAndYear(month, year);

        BigDecimal aiChat = transactionRepository.sumAmountByTypeAndMonth(TransactionType.AI_CHAT, month, year);
        BigDecimal postCharges = transactionRepository.sumAmountByTypeAndMonth(TransactionType.POST_CHARGE, month, year);
        BigDecimal listingFees = transactionRepository.sumAmountByTypeAndMonth(TransactionType.LISTING_FEE, month, year);

        int prevMonth = month == 1 ? 12 : month - 1;
        int prevYear = month == 1 ? year - 1 : year;
        BigDecimal prevRevenue = coalesce(transactionRepository.getRevenue(prevMonth, prevYear));

        double growthPercent = 0.0;
        if (prevRevenue.compareTo(BigDecimal.ZERO) > 0) {
            growthPercent = revenue.subtract(prevRevenue)
                    .divide(prevRevenue, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        }

        return new AdminRevenueDTO(month, year, revenue, refunds, netRevenue, txCount,
                aiChat, postCharges, listingFees, prevRevenue, growthPercent);
    }

    @Override
    public AdminCreditUsageDTO getCreditUsage(int month, int year) {
        BigDecimal purchases = transactionRepository.sumAmountByTypeAndMonth(TransactionType.PURCHASE, month, year);
        BigDecimal aiChat = transactionRepository.sumAmountByTypeAndMonth(TransactionType.AI_CHAT, month, year);
        BigDecimal postCharges = transactionRepository.sumAmountByTypeAndMonth(TransactionType.POST_CHARGE, month, year);
        BigDecimal listingFees = transactionRepository.sumAmountByTypeAndMonth(TransactionType.LISTING_FEE, month, year);
        BigDecimal refunds = transactionRepository.sumAmountByTypeAndMonth(TransactionType.REFUND, month, year);

        long purchaseCount = transactionRepository.countByTypeAndMonth(TransactionType.PURCHASE, month, year);
        long aiChatCount = transactionRepository.countByTypeAndMonth(TransactionType.AI_CHAT, month, year);
        long postCount = transactionRepository.countByTypeAndMonth(TransactionType.POST_CHARGE, month, year);
        long listingCount = transactionRepository.countByTypeAndMonth(TransactionType.LISTING_FEE, month, year);
        long refundCount = transactionRepository.countByTypeAndMonth(TransactionType.REFUND, month, year);

        return new AdminCreditUsageDTO(month, year, purchases, aiChat, postCharges, listingFees, refunds,
                purchaseCount, aiChatCount, postCount, listingCount, refundCount);
    }

    @Override
    public List<CreditTransaction> getTransactionsByMonth(int month, int year) {
        return transactionRepository.findAllByMonthAndYear(month, year);
    }

    @Override
    public List<AdminUserDTO> getTopSpenders(int limit) {
        return walletRepository
                .findAllByOrderByTotalSpentDesc(PageRequest.of(0, limit))
                .stream()
                .map(w -> new AdminUserDTO(
                        w.getUserId(),
                        w.getBalance(),
                        w.getReservedBalance(),
                        w.getTotalSpent(),
                        w.getStatus(),
                        w.getCreatedAt()))
                .toList();
    }

    @Override
    public AdminWalletStatsDTO getWalletStats() {
        long total = walletRepository.count();
        BigDecimal totalBalance = walletRepository.sumAllBalances();
        BigDecimal totalReserved = walletRepository.sumAllReservedBalances();
        BigDecimal average = total > 0
                ? totalBalance.divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        long zeroBalance = walletRepository.countByBalance(BigDecimal.ZERO);

        return new AdminWalletStatsDTO(total, totalBalance, totalReserved, average, zeroBalance);
    }

    private BigDecimal coalesce(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
