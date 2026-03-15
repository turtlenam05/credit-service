package com.dathq.swd302.creditservice.scheduler;

import com.dathq.swd302.creditservice.entity.DailyUsage;
import com.dathq.swd302.creditservice.repository.DailyUsageRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyUsageResetScheduler {
    private static final String KEY_PATTERN = "daily_msg:*";
    private static final String KEY_PREFIX = "daily_msg:";

    private final RedisTemplate<String, String> redisTemplate;
    private final DailyUsageRepository dailyUsageRepository;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void resetDailyMessageCounts() {
        log.info("[DailyUsageReset] Starting midnight reset job");

        LocalDate yesterday = LocalDate.now().minusDays(1);
        Set<String> keys = redisTemplate.keys(KEY_PATTERN);

        if(keys == null || keys.isEmpty()) {
            log.info("[DailyUsageReset] No No active daily_msg keys found. Nothing to snapshot.");
            return;
        }

        int snapshotCount = 0;
        int resetCount = 0;

        for (String key : keys) {
            try {
                String rawValue = redisTemplate.opsForValue().get(key);
                if(rawValue == null) continue;

                int count = Integer.parseInt(rawValue);

                String userIdStr = key.substring(KEY_PREFIX.length());
                UUID userId = UUID.fromString(userIdStr);
                long userIdLong = userId.getLeastSignificantBits();

                DailyUsage record = dailyUsageRepository.findByUserIdAndUsageDate(userIdLong, yesterday)
                        .orElse(DailyUsage.builder()
                                .userId(userIdLong)
                                .usageDate(yesterday)
                                .freeMessageCount(0)
                                .build());

                record.setFreeMessageCount(count);
                dailyUsageRepository.save(record);
                snapshotCount++;

                redisTemplate.delete(key);
                resetCount++;
            } catch (Exception ex) {
                log.error("[DailyUsageReset] Failed to process key={}: {}", key, ex.getMessage(), ex);
            }
        }

        log.info("[DailyUsageReset] Completed. Snapshotted={} Reset={} date={}",
                snapshotCount, resetCount, yesterday);
    }
}
