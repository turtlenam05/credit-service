package com.dathq.swd302.creditservice.repository;

import com.dathq.swd302.creditservice.entity.CreditTransaction;
import com.dathq.swd302.creditservice.entity.TransactionType;
import jakarta.transaction.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
}