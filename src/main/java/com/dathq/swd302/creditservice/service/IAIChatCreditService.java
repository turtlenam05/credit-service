// IAIChatCreditService.java  (called by AI Chat Service via Feign/REST)
package com.dathq.swd302.creditservice.service;

import com.dathq.swd302.creditservice.dto.ChatUsageDTO;

import java.util.UUID;

public interface IAIChatCreditService {
    boolean canSendFreeMessage(UUID userId);
    boolean consumeMessage(UUID userId);
    void resetDailyCount(UUID userId);
    void syncToDb(UUID userId, int count);
    ChatUsageDTO getUsage(UUID userId);
}
