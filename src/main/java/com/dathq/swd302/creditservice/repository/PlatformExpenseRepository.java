package com.dathq.swd302.creditservice.repository;

import com.dathq.swd302.creditservice.entity.PlatformExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PlatformExpenseRepository extends JpaRepository<PlatformExpense, Long> {
    List<PlatformExpense> findByMonthAndYear(int month, int year);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM PlatformExpense e WHERE e.month = :month AND e.year = :year")
    BigDecimal sumByMonthAndYear(@Param("month") int month, @Param("year") int year);
}
