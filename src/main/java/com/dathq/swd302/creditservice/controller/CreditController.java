package com.dathq.swd302.creditservice.controller;

import com.dathq.swd302.creditservice.entity.CreditTransaction;
import com.dathq.swd302.creditservice.entity.UserWallet;
import com.dathq.swd302.creditservice.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author matve
 */
@RestController
@RequestMapping("/api/v1/credits")
@RequiredArgsConstructor
public class CreditController {

    private final ICreditService creditService;
    private final IPaymentService paymentService;
    private final IAIChatCreditService aiChatCreditService;

    // ==================== WALLET ====================

    @GetMapping("/balance")
    public ResponseEntity<UserWallet> getBalance(
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(creditService.getWallet(userId));
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<CreditTransaction>> getTransactionHistory(
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(creditService.getTransactionHistory(userId));
    }

    // ==================== TOP-UP ====================

    @PostMapping("/topup/initiate")
    public ResponseEntity<?> initiateTopUp(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody Map<String, Object> body) {
        try {
            int amount = (int) body.get("amount");
            String checkoutUrl = paymentService.createPaymentLink(userId, amount);
            return ResponseEntity.ok(Map.of("checkoutUrl", checkoutUrl));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== AI CHAT ====================

    @GetMapping("/chat/quota")
    public ResponseEntity<Map<String, Object>> getAIChatQuota(
            @RequestHeader("X-User-Id") UUID userId) {
        int used = creditService.getDailyMessageCount(userId);
        boolean canFree = aiChatCreditService.canSendFreeMessage(userId);
        UserWallet wallet = creditService.getWallet(userId);

        return ResponseEntity.ok(Map.of(
                "dailyMessageUsed", used,
                "freeRemaining", Math.max(0, 30 - used),
                "canSendFreeMessage", canFree,
                "creditBalance", wallet.getBalance()
        ));
    }

    @PostMapping("/usage/chat")
    public ResponseEntity<Map<String, Object>> consumeAIMessage(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody Map<String, Object> body) {
        boolean canFree = aiChatCreditService.canSendFreeMessage(userId);
        boolean success = aiChatCreditService.consumeMessage(userId);
        int used = creditService.getDailyMessageCount(userId);

        if (!success) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Không đủ credit. Vui lòng nạp thêm."
            ));
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "dailyMessageUsed", used,
                "freeRemaining", Math.max(0, 30 - used),
                "isFreeMessage", canFree,
                "message", canFree ? "Tin nhắn miễn phí" : "Đã trừ 1 credit"
        ));
    }

    // ==================== TEST ONLY ====================

    @PostMapping("/recharge/test")
    public ResponseEntity<UserWallet> rechargeTest(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam Double amount) {
        return ResponseEntity.ok(creditService.rechargeBalance(userId, amount));
    }
}