package com.dathq.swd302.creditservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @author matve
 */

@Entity
@Table(name = "transaction")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id")
    private UserWallet wallet;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private TransactionType type; // PURCHASE, AI_CHAT, LISTING_FEE, REFUND

    private String referenceType; // e.g., "PAYOS_ORDER", "LISTING", "AI_SESSION"
    private String referenceId;   // ID của đối tượng liên quan từ service khác

    @Enumerated(EnumType.STRING)
    private TransactionStatus status; // PENDING, SUCCESS, FAILED

    private String notes;
    private LocalDateTime createdAt = LocalDateTime.now();
}

