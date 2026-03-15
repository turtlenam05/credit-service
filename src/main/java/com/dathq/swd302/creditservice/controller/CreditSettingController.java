package com.dathq.swd302.creditservice.controller;


import com.dathq.swd302.creditservice.dto.CreditSettingResponse;
import com.dathq.swd302.creditservice.dto.UpdateAllSettingsRequest;
import com.dathq.swd302.creditservice.dto.UpdateCreditSettingRequest;
import com.dathq.swd302.creditservice.entity.CreditSetting;
import com.dathq.swd302.creditservice.security.JwtClaims;
import com.dathq.swd302.creditservice.security.JwtUser;
import com.dathq.swd302.creditservice.service.ICreditSettingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/credit-settings")
@RequiredArgsConstructor
@Slf4j
public class CreditSettingController {

    private final ICreditSettingService creditSettingService;

    // GET all settings
    @GetMapping
    public ResponseEntity<List<CreditSettingResponse>> getAllSettings() {
        return ResponseEntity.ok(creditSettingService.getAllSettings());
    }

    // GET one setting
    @GetMapping("/{key}")
    public ResponseEntity<CreditSettingResponse> getSetting(
            @PathVariable CreditSetting.SettingKey key) {
        return ResponseEntity.ok(creditSettingService.getSetting(key));
    }

    // PUT update one setting
    @PutMapping("/{key}")
    public ResponseEntity<CreditSettingResponse> updateSetting(
            @PathVariable CreditSetting.SettingKey key,
            @RequestBody @Valid UpdateCreditSettingRequest request,
            @JwtUser JwtClaims claims) {

        if (!"ADMIN".equals(claims.getRole().toUpperCase())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(
                creditSettingService.updateSetting(key, request, claims.getUserId())
        );
    }

    @PutMapping("/bulk")
    public ResponseEntity<Void> updateAllSettings(
            @RequestBody @Valid UpdateAllSettingsRequest request,
            @JwtUser JwtClaims claims) {

        if (!"ADMIN".equals(claims.getRole().toUpperCase())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        creditSettingService.updateAllSettings(request, claims.getUserId());
        return ResponseEntity.ok().build();
    }

    // POST reset daily count for a user (admin/test)
    @PostMapping("/ai-chat/reset-daily/{userId}")
    public ResponseEntity<Void> resetDailyCount(@PathVariable UUID userId,
                                                @JwtUser JwtClaims claims) {
        if (!"ADMIN".equals(claims.getRole().toUpperCase())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        // call AIChatCreditService
        return ResponseEntity.ok().build();
    }
}
