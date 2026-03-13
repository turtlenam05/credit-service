package com.dathq.swd302.creditservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_audit_entry")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TransactionAuditEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The reconciliation period this audit belongs to. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reconciliation_id", nullable = false)
    private MonthlyReconciliation reconciliation;

    /** Link back to the source credit transaction (e.g. TXN-48201). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private CreditTransaction transaction;

    /** Human-readable label shown in the audit table (e.g. "Credit purchase — Premium listing package"). */
    @Column(nullable = false)
    private String description;

    /** Amount recorded in the credit system. */
    @Column(nullable = false)
    private BigDecimal systemAmount;

    /**
     * Amount recorded by the payment gateway.
     * NULL means the gateway has no record → UNMATCHED.
     */
    @Column
    private BigDecimal gatewayAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionMatchStatus matchStatus;

    /** Admin note added during investigation (optional). */
    @Column(columnDefinition = "TEXT")
    private String adminNote;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
