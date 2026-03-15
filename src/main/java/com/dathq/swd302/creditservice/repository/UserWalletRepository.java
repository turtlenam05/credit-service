package com.dathq.swd302.creditservice.repository;

import com.dathq.swd302.creditservice.entity.UserWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * @author matve
 */
@Repository
public interface UserWalletRepository extends JpaRepository<UserWallet, Long> {
    Optional<UserWallet> findByUserId(UUID userId);

    List<UserWallet> findAllByOrderByTotalSpentDesc(Pageable pageable);

    @Query("SELECT COALESCE(SUM(w.balance), 0) FROM UserWallet w")
    BigDecimal sumAllBalances();

    @Query("SELECT COALESCE(SUM(w.reservedBalance), 0) FROM UserWallet w")
    BigDecimal sumAllReservedBalances();

    long countByBalance(BigDecimal balance);
}