package com.dathq.swd302.creditservice.repository;

import com.dathq.swd302.creditservice.entity.CreditTransaction;
import com.dathq.swd302.creditservice.entity.TransactionType;
import jakarta.transaction.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * @author matve
 */
@Repository
public interface TransactionRepository extends JpaRepository<CreditTransaction, Long> {
    List<CreditTransaction> findByWallet_UserIdOrderByCreatedAtDesc(UUID userId);
    CreditTransaction findByReferenceId(String referenceId);

    boolean existsByWallet_UserIdAndType(UUID userId, TransactionType transactionType);

    boolean existsByWallet_UserIdAndReferenceType(UUID userId, String firstPost);

    @Query("""
    SELECT SUM(t.amount)
    FROM CreditTransaction t
    WHERE t.type='PURCHASE'
    AND MONTH(t.createdAt)=:month
    AND YEAR(t.createdAt)=:year
    """)
    BigDecimal getRevenue(int month, int year);

    @Query("""
    SELECT SUM(t.amount)
    FROM CreditTransaction  t  
    WHERE t.type='REFUND'
    AND MONTH(t.createdAt)=:month
    AND YEAR(t.createdAt)=:year
    """)
    BigDecimal getRefunds(int month, int year);

    /** Tất cả các giao dịch MUA hàng thành công trong một tháng nhất định (được sử dụng để lập các bút toán kiểm toán). */
    @Query("SELECT t FROM CreditTransaction t " +
            "WHERE t.type = 'PURCHASE' AND t.status = 'SUCCESS' " +
            "AND MONTH(t.createdAt) = :month AND YEAR(t.createdAt) = :year " +
            "ORDER BY t.createdAt DESC")
    List<CreditTransaction> findPurchasesByMonth(@Param("month") int month, @Param("year") int year);

    /** Tất cả các giao dịch trong một tháng (để phục vụ cho việc điều tra chi tiết). */
    @Query("SELECT t FROM CreditTransaction t " +
            "WHERE MONTH(t.createdAt) = :month AND YEAR(t.createdAt) = :year " +
            "ORDER BY t.createdAt DESC")
    List<CreditTransaction> findAllByMonthAndYear(@Param("month") int month, @Param("year") int year);

    @Query("SELECT COUNT(t) FROM CreditTransaction t " +
            "WHERE MONTH(t.createdAt) = :month AND YEAR(t.createdAt) = :year")
    int countByMonthAndYear(@Param("month") int month, @Param("year") int year);

    // ─── Admin analytics ──────────────────────────────────────────────────────

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM CreditTransaction t WHERE t.type = :type")
    BigDecimal sumAmountByType(@Param("type") TransactionType type);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM CreditTransaction t " +
            "WHERE t.type = :type AND MONTH(t.createdAt) = :month AND YEAR(t.createdAt) = :year")
    BigDecimal sumAmountByTypeAndMonth(@Param("type") TransactionType type,
                                       @Param("month") int month,
                                       @Param("year") int year);

    @Query("SELECT COUNT(t) FROM CreditTransaction t " +
            "WHERE t.type = :type AND MONTH(t.createdAt) = :month AND YEAR(t.createdAt) = :year")
    long countByTypeAndMonth(@Param("type") TransactionType type,
                              @Param("month") int month,
                              @Param("year") int year);
}