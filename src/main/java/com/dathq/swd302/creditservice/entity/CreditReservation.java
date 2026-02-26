package com.dathq.swd302.creditservice.entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @author matve
 */

@Entity
@Table(name = "credit_reservation")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreditReservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reservationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id")
    private UserWallet wallet;

    private String listingId;
    private BigDecimal amount = new BigDecimal("10");

    private String status; // PENDING, CONSUMED (trừ hẳn), RELEASED (hoàn lại)
    private LocalDateTime createdAt = LocalDateTime.now();
}
