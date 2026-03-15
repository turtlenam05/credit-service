package com.dathq.swd302.creditservice.service;

import com.dathq.swd302.creditservice.dto.*;
import com.dathq.swd302.creditservice.entity.*;
import com.dathq.swd302.creditservice.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;

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
    public List<CreditTransaction> getTransactions(AdminTransactionFilter filter) {
        validateTransactionFilter(filter);

        Specification<CreditTransaction> specification = (root, query, cb) -> cb.conjunction();

        if (filter.month() != null && filter.year() != null) {
            YearMonth yearMonth = YearMonth.of(filter.year(), filter.month());
            LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
            LocalDateTime end = yearMonth.plusMonths(1).atDay(1).atStartOfDay();

            specification = specification.and((root, query, cb) -> cb.and(
                    cb.greaterThanOrEqualTo(root.get("createdAt"), start),
                    cb.lessThan(root.get("createdAt"), end)
            ));
        }

        if (filter.userIds() != null && !filter.userIds().isEmpty()) {
            specification = specification.and((root, query, cb) -> root.get("wallet").get("userId").in(filter.userIds()));
        }

        if (filter.type() != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("type"), filter.type()));
        }

        if (filter.status() != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("status"), filter.status()));
        }

        if (hasText(filter.referenceType())) {
            specification = specification.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("referenceType")), contains(filter.referenceType())));
        }

        if (hasText(filter.referenceId())) {
            specification = specification.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("referenceId")), contains(filter.referenceId())));
        }

        if (filter.minAmount() != null) {
            specification = specification.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("amount"), filter.minAmount()));
        }

        if (filter.maxAmount() != null) {
            specification = specification.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("amount"), filter.maxAmount()));
        }

        if (filter.fromDate() != null) {
            specification = specification.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("createdAt"), filter.fromDate()));
        }

        if (filter.toDate() != null) {
            specification = specification.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("createdAt"), filter.toDate()));
        }

        return transactionRepository.findAll(specification, buildSort(filter.sortBy(), filter.sortDir()));
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

    private void validateTransactionFilter(AdminTransactionFilter filter) {
        if ((filter.month() == null) != (filter.year() == null)) {
            throw new IllegalArgumentException("month and year must be provided together");
        }

        if (filter.month() != null && (filter.month() < 1 || filter.month() > 12)) {
            throw new IllegalArgumentException("month must be between 1 and 12");
        }

        if (filter.minAmount() != null && filter.maxAmount() != null
                && filter.minAmount().compareTo(filter.maxAmount()) > 0) {
            throw new IllegalArgumentException("minAmount cannot be greater than maxAmount");
        }

        if (filter.fromDate() != null && filter.toDate() != null
                && filter.fromDate().isAfter(filter.toDate())) {
            throw new IllegalArgumentException("fromDate cannot be after toDate");
        }
    }

    private Sort buildSort(String sortBy, String sortDir) {
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String property = mapSortProperty(sortBy);
        return Sort.by(direction, property);
    }

    private String mapSortProperty(String sortBy) {
        String normalized = hasText(sortBy) ? sortBy.trim().toLowerCase(Locale.ROOT) : "date";

        return switch (normalized) {
            case "amount" -> "amount";
            case "date", "createdat", "created_at" -> "createdAt";
            case "status" -> "status";
            case "type" -> "type";
            case "userid", "user", "user_id" -> "wallet.userId";
            case "referencetype", "reference_type" -> "referenceType";
            case "referenceid", "reference_id" -> "referenceId";
            default -> throw new IllegalArgumentException("Unsupported sortBy value: " + sortBy);
        };
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String contains(String value) {
        return "%" + value.toLowerCase(Locale.ROOT).trim() + "%";
    }
}
