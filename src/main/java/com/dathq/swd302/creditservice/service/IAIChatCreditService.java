// IAIChatCreditService.java  (called by AI Chat Service via Feign/REST)
package com.dathq.swd302.creditservice.service;

import java.util.UUID;

public interface IAIChatCreditService {
    boolean canSendFreeMessage(UUID userId);
    boolean consumeMessage(UUID userId);
    void resetDailyCount(UUID userId);
}
