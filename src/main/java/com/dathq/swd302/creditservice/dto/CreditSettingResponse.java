package com.dathq.swd302.creditservice.dto;


import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class CreditSettingResponse {
    private Long id;
    private String settingKey;
    private int value;
    private String description;
    private LocalDateTime updatedAt;
    private UUID updatedBy;
}
