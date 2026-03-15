package com.dathq.swd302.creditservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static com.dathq.swd302.creditservice.entity.CreditSetting.SettingKey.AI_CHAT_COST_PER_MSG;
import static com.dathq.swd302.creditservice.entity.CreditSetting.SettingKey.AI_CHAT_FREE_LIMIT;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIChatCreditServiceImpl implements IAIChatCreditService{

    private final ICreditService creditService;
    private final ICreditSettingService settingService;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public boolean canSendFreeMessage(UUID userId) {
        int limit = settingService.getValue(AI_CHAT_FREE_LIMIT);
        return creditService.getDailyMessageCount(userId) < limit;
    }

    @Override
    public boolean consumeMessage(UUID userId) {
        int limit = settingService.getValue(AI_CHAT_FREE_LIMIT);
        int cost = settingService.getValue(AI_CHAT_COST_PER_MSG);
        int currentCount = creditService.getDailyMessageCount(userId);

        if (currentCount < limit) {
            // Free message
            creditService.incrementDailyMessageCount(userId);
            return true;
        }

        // Vượt 30 tin → trừ 1 credit
        boolean deducted = creditService.deductCredit(userId, cost);
        if (deducted) {
            creditService.incrementDailyMessageCount(userId);
        }
        return deducted;
    }

    @Override
    public void resetDailyCount(UUID userId) {
        // Fix: actually delete the Redis key instead of incrementing
        String key = "daily_msg_count:" + userId;
        redisTemplate.delete(key);
        log.info("Daily message count reset for user: {}", userId);
    }
}
