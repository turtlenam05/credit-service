package com.dathq.swd302.creditservice.service;

import com.dathq.swd302.creditservice.dto.CreditSettingResponse;
import com.dathq.swd302.creditservice.dto.UpdateAllSettingsRequest;
import com.dathq.swd302.creditservice.dto.UpdateCreditSettingRequest;
import com.dathq.swd302.creditservice.entity.CreditSetting;

import java.util.List;
import java.util.UUID;

public interface ICreditSettingService {
    List<CreditSettingResponse> getAllSettings();
    CreditSettingResponse getSetting(CreditSetting.SettingKey key);
    CreditSettingResponse updateSetting(CreditSetting.SettingKey key, UpdateCreditSettingRequest request, UUID adminId);
    void updateAllSettings(UpdateAllSettingsRequest request, UUID adminId);
    int getValue(CreditSetting.SettingKey key);
}
