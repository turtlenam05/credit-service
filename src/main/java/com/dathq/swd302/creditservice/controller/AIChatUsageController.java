package com.dathq.swd302.creditservice.controller;

import com.dathq.swd302.creditservice.dto.ChatUsageDTO;
import com.dathq.swd302.creditservice.repository.DailyUsageRepository;
import com.dathq.swd302.creditservice.scheduler.DailyUsageResetScheduler;
import com.dathq.swd302.creditservice.service.IAIChatCreditService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class AIChatUsageController {
    private final IAIChatCreditService chatCreditService;
    private final DailyUsageResetScheduler scheduler;

    @GetMapping("/usage/{userId}")
    public ResponseEntity<ChatUsageDTO> getUsage(@PathVariable UUID userId) {
        return ResponseEntity.ok(chatCreditService.getUsage(userId));
    }

    //    test api
    @PostMapping("/reset/all")
    public ResponseEntity<String> resetAll() {
        scheduler.resetDailyMessageCounts();
        return ResponseEntity.ok("Full daily reset triggered successfully.");
    }
}
