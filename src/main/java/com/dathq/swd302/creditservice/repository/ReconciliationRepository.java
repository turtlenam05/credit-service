package com.dathq.swd302.creditservice.repository;

import com.dathq.swd302.creditservice.entity.MonthlyReconciliation;
import com.dathq.swd302.creditservice.entity.ReconciliationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReconciliationRepository extends JpaRepository<MonthlyReconciliation, Long> {
    Optional<MonthlyReconciliation> findByMonthAndYear(int month, int year);
    List<MonthlyReconciliation> findAllByOrderByYearDescMonthDesc();
    List<MonthlyReconciliation> findByStatus(ReconciliationStatus status);
}
