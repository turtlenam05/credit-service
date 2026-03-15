package com.dathq.swd302.creditservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatUsageDTO {
    private Long userId;
    private LocalDate usageDate;
    private int messageCountToday;
    private int freeLimit;
    private int freeRemaining;
    private boolean withinFreeLimit;
}
