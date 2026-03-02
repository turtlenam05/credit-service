package com.dathq.swd302.creditservice.repository;

import com.dathq.swd302.creditservice.entity.CreditTransaction;
import com.dathq.swd302.creditservice.entity.TransactionType;
import jakarta.transaction.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}