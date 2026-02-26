package com.dathq.swd302.creditservice.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

/**
 * @author matve
 */

@Entity
@Table(name = "daily_usage")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DailyUsage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long usageId;

    private Long userId;
    private LocalDate usageDate;
    private int freeMessageCount = 0; // Tối đa 30
}
