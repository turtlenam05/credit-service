package com.dathq.swd302.creditservice.repository;

import com.dathq.swd302.creditservice.entity.CreditSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CreditSettingRepository  extends JpaRepository<CreditSetting, Long> {
    Optional<CreditSetting> findBySettingKey(CreditSetting.SettingKey settingKey);

}
