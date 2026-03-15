package com.dathq.swd302.creditservice.service;

import com.dathq.swd302.creditservice.dto.ChatUsageDTO;
import com.dathq.swd302.creditservice.entity.DailyUsage;
import com.dathq.swd302.creditservice.repository.DailyUsageRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AIChatCreditServiceImpl implements IAIChatCreditService{

    private final ICreditService creditService;
    private final DailyUsageRepository dailyUsageRepository;
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
        boolean deducted = creditService.deductCredit(userId, 1000);
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

        int liveCount = creditService.getDailyMessageCount(userId);

        // If Redis key has already expired (e.g. just after midnight before any
        // message is sent), fall back to today's DB record if it exists.
        if (liveCount == 0) {
            liveCount = dailyUsageRepository
                    .findByUserIdAndUsageDate(userIdLong, LocalDate.now())
                    .map(DailyUsage::getFreeMessageCount)
                    .orElse(0);
        }

        int freeRemaining = Math.max(0, FREE_MESSAGE_LIMIT - liveCount);

        return ChatUsageDTO.builder()
                .userId(userIdLong)
                .usageDate(LocalDate.now())
                .messageCountToday(liveCount)
                .freeLimit(FREE_MESSAGE_LIMIT)
                .freeRemaining(freeRemaining)
                .withinFreeLimit(liveCount < FREE_MESSAGE_LIMIT)
                .build();
    }
}
