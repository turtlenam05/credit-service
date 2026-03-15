package com.dathq.swd302.creditservice.service;

import com.dathq.swd302.creditservice.dto.CreditSettingResponse;
import com.dathq.swd302.creditservice.dto.UpdateAllSettingsRequest;
import com.dathq.swd302.creditservice.dto.UpdateCreditSettingRequest;
import com.dathq.swd302.creditservice.entity.CreditSetting;
import com.dathq.swd302.creditservice.repository.CreditSettingRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.dathq.swd302.creditservice.entity.CreditSetting.SettingKey.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreditSettingServiceImpl implements ICreditSettingService{

    private final CreditSettingRepository creditSettingRepository;
    // Cache in memory to avoid DB hit every message
    private final Map<CreditSetting.SettingKey, Integer> cache = new ConcurrentHashMap<>();

    @PostConstruct
    public void loadCache() {
        creditSettingRepository.findAll()
                .forEach(s -> cache.put(s.getSettingKey(), s.getValue()));
        log.info("Credit settings loaded: {}", cache);
    }

    @Override
    public List<CreditSettingResponse> getAllSettings() {
        return creditSettingRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CreditSettingResponse getSetting(CreditSetting.SettingKey key) {
        return creditSettingRepository.findBySettingKey(key)
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException("Setting not found: " + key));
    }

    @Override
    public CreditSettingResponse updateSetting(CreditSetting.SettingKey key, UpdateCreditSettingRequest request, UUID adminId) {
        CreditSetting setting = creditSettingRepository.findBySettingKey(key)
                .orElseThrow(() -> new RuntimeException("Setting not found: " + key));

        setting.setValue(request.getValue());
        setting.setUpdatedAt(LocalDateTime.now());
        setting.setUpdatedBy(adminId);
        if (request.getDescription() != null) {
            setting.setDescription(request.getDescription());
        }

        CreditSetting saved = creditSettingRepository.save(setting);
        cache.put(key, request.getValue()); // update cache
        log.info("Setting [{}] updated to {} by admin {}", key, request.getValue(), adminId);

        return toResponse(saved);
    }

    @Override
    public void updateAllSettings(UpdateAllSettingsRequest request, UUID adminId) {
        if (request.getPostCostBasic() != null)
            updateSetting(POST_COST_BASIC, toRequest(request.getPostCostBasic()), adminId);
        if (request.getPostCostPremiumAdd() != null)
            updateSetting(POST_COST_PREMIUM_ADD, toRequest(request.getPostCostPremiumAdd()), adminId);
        if (request.getAiChatFreeLimit() != null)
            updateSetting(AI_CHAT_FREE_LIMIT, toRequest(request.getAiChatFreeLimit()), adminId);
        if (request.getAiChatCostPerMsg() != null)
            updateSetting(AI_CHAT_COST_PER_MSG, toRequest(request.getAiChatCostPerMsg()), adminId);
    }

    @Override
    public int getValue(CreditSetting.SettingKey key) {
        return cache.getOrDefault(key, getDefaultValue(key));
    }

    private int getDefaultValue(CreditSetting.SettingKey key) {
        return switch (key) {
            case POST_COST_BASIC -> 10000;
            case POST_COST_PREMIUM_ADD -> 50000;
            case AI_CHAT_FREE_LIMIT -> 30;
            case AI_CHAT_COST_PER_MSG -> 1000;
        };
    }
    private UpdateCreditSettingRequest toRequest(int value) {
        UpdateCreditSettingRequest r = new UpdateCreditSettingRequest();
        r.setValue(value);
        return r;
    }
    private CreditSettingResponse toResponse(CreditSetting s) {
        return CreditSettingResponse.builder()
                .id(s.getId())
                .settingKey(s.getSettingKey().name())
                .value(s.getValue())
                .description(s.getDescription())
                .updatedAt(s.getUpdatedAt())
                .updatedBy(s.getUpdatedBy())
                .build();
    }
}
