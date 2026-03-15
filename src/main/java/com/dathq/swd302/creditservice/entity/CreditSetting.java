package com.dathq.swd302.creditservice.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "credit_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "setting_key", unique = true, nullable = false)
    @Enumerated(EnumType.STRING)
    private SettingKey settingKey;

    @Column(name = "value", nullable = false)
    private int value;

    @Column(name = "description")
    private String description;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    public enum SettingKey {
        POST_COST_BASIC,       // base post cost (10,000)
        POST_COST_PREMIUM_ADD, // extra cost for premium post (50,000)
        AI_CHAT_FREE_LIMIT,    // free messages per day (30)
        AI_CHAT_COST_PER_MSG   // credit cost per extra message (1,000)
    }
}
