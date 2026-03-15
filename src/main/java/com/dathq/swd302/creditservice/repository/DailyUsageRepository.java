package com.dathq.swd302.creditservice.repository;

import com.dathq.swd302.creditservice.entity.DailyUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyUsageRepository extends JpaRepository<DailyUsage, Long> {
    Optional<DailyUsage> findByUserIdAndUsageDate(Long userId, LocalDate usageDate);
    List<DailyUsage> findByUsageDate(LocalDate usageDate);
    List<DailyUsage> findByUserIdOrderByUsageDateDesc(Long userId);

    @Modifying
    @Query("DELETE FROM DailyUsage d WHERE d.usageDate < :cutoff")
    void deleteOlderThan(@Param("cutoff") LocalDate cutoff);
}
