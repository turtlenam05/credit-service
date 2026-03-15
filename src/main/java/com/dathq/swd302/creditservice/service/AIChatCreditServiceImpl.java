package com.dathq.swd302.creditservice.service;

import com.dathq.swd302.creditservice.dto.ChatUsageDTO;
import com.dathq.swd302.creditservice.entity.DailyUsage;
import com.dathq.swd302.creditservice.repository.DailyUsageRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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
    private final DailyUsageRepository dailyUsageRepository;


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

    @Override
    @Transactional
    public void syncToDb(UUID userId, int count) {
        long userIdLong = userId.getLeastSignificantBits();
        LocalDate today = LocalDate.now();

        DailyUsage record = dailyUsageRepository
                .findByUserIdAndUsageDate(userIdLong, today)
                .orElse(DailyUsage.builder()
                        .userId(userIdLong)
                        .usageDate(today)
                        .freeMessageCount(0)
                        .build());

        record.setFreeMessageCount(count);
        dailyUsageRepository.save(record);
    }

    @Override
    public ChatUsageDTO getUsage(UUID userId) {
        long userIdLong = userId.getLeastSignificantBits();
        int limit = settingService.getValue(AI_CHAT_FREE_LIMIT);
        int liveCount = creditService.getDailyMessageCount(userId);

        // If Redis key has already expired (e.g. just after midnight before any
        // message is sent), fall back to today's DB record if it exists.
        if (liveCount == 0) {
            liveCount = dailyUsageRepository
                    .findByUserIdAndUsageDate(userIdLong, LocalDate.now())
                    .map(DailyUsage::getFreeMessageCount)
                    .orElse(0);
        }

        int freeRemaining = Math.max(0, limit - liveCount);

        return ChatUsageDTO.builder()
                .userId(userIdLong)
                .usageDate(LocalDate.now())
                .messageCountToday(liveCount)
                .freeLimit(limit)
                .freeRemaining(freeRemaining)
                .withinFreeLimit(liveCount < limit)
                .build();
    }
}
