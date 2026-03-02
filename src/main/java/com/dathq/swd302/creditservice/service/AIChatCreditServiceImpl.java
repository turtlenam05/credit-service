package com.dathq.swd302.creditservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AIChatCreditServiceImpl implements IAIChatCreditService{

    private final ICreditService creditService;
    private static final int FREE_MESSAGE_LIMIT = 30;

    @Override
    public boolean canSendFreeMessage(UUID userId) {
        return creditService.getDailyMessageCount(userId) < FREE_MESSAGE_LIMIT;
    }

    @Override
    public boolean consumeMessage(UUID userId) {
        int currentCount = creditService.getDailyMessageCount(userId);

        if (currentCount < FREE_MESSAGE_LIMIT) {
            // Free message
            creditService.incrementDailyMessageCount(userId);
            return true;
        }

        // Vượt 30 tin → trừ 1 credit
        boolean deducted = creditService.deductCredit(userId, 1);
        if (deducted) {
            creditService.incrementDailyMessageCount(userId);
        }
        return deducted;
    }

    @Override
    public void resetDailyCount(UUID userId) {
        // Redis tự reset theo TTL đến 00:00 GMT+7
        // Method này dùng khi cần manual reset (admin/test)
        creditService.incrementDailyMessageCount(userId);
    }
}
