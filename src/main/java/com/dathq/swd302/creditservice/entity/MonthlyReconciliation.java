package com.dathq.swd302.creditservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "monthly_reconciliation", uniqueConstraints = @UniqueConstraint(columnNames = {"month", "year"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyReconciliation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int month;

    @Column(nullable = false)
    private int year;

    @Column(nullable = false)
    private BigDecimal totalCreditSold = BigDecimal.ZERO;       // PURCHASE txns

    @Column(nullable = false)
    private BigDecimal totalRefunded = BigDecimal.ZERO;         // REFUND txns

    @Column(nullable = false)
    private BigDecimal totalGatewayReceived = BigDecimal.ZERO;  // from payment gateway

    @Column(nullable = false)
    private BigDecimal totalExpenses = BigDecimal.ZERO;         // platform costs

    @Column(nullable = false)
    private BigDecimal discrepancy = BigDecimal.ZERO;           // gatewayReceived − creditSold

    @Column(nullable = false)
    private BigDecimal netProfit = BigDecimal.ZERO;             // creditSold − refunded − expenses

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReconciliationStatus status = ReconciliationStatus.PENDING;

    /** Admin explanation when resolving a discrepancy. */
    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
