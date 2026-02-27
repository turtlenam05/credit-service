package com.dathq.swd302.creditservice.repository;

import com.dathq.swd302.creditservice.entity.CreditTransaction;
import jakarta.transaction.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author matve
 */
@Repository
public interface TransactionRepository extends JpaRepository<CreditTransaction, Long> {
    List<CreditTransaction> findByWallet_UserIdOrderByCreatedAtDesc(Long userId);
    CreditTransaction findByReferenceId(String referenceId);
}