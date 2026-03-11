package com.dathq.swd302.creditservice.controller;

import com.dathq.swd302.creditservice.entity.CreditTransaction;
import com.dathq.swd302.creditservice.entity.UserWallet;
import com.dathq.swd302.creditservice.security.JwtClaims;
import com.dathq.swd302.creditservice.security.JwtUser;
import com.dathq.swd302.creditservice.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
            @JwtUser JwtClaims claims) {
        return ResponseEntity.ok(creditService.getWallet(claims.getUserId()));
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<CreditTransaction>> getTransactionHistory(
            @JwtUser JwtClaims claims) {
        return ResponseEntity.ok(creditService.getTransactionHistory(claims.getUserId()));
    }

    // ==================== TOP-UP ====================

    @PostMapping("/topup/initiate")
    public ResponseEntity<?> initiateTopUp(
            @JwtUser JwtClaims claims,
            @RequestBody Map<String, Object> body) {
        try {
            int amount = (int) body.get("amount");
            String checkoutUrl = paymentService.createPaymentLink(claims.getUserId(), amount);
            return ResponseEntity.ok(Map.of("checkoutUrl", checkoutUrl));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/wallet")
    public ResponseEntity<UserWallet> createWallet(@JwtUser JwtClaims claims) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(creditService.createWallet(claims.getUserId()));
    }

    // ==================== AI CHAT ====================

    @GetMapping("/chat/quota")
    public ResponseEntity<Map<String, Object>> getAIChatQuota(
            @JwtUser JwtClaims claims) {
        int used = creditService.getDailyMessageCount(claims.getUserId());
        boolean canFree = aiChatCreditService.canSendFreeMessage(claims.getUserId());
        UserWallet wallet = creditService.getWallet(claims.getUserId());

        return ResponseEntity.ok(Map.of(
                "dailyMessageUsed", used,
                "freeRemaining", Math.max(0, 30 - used),
                "canSendFreeMessage", canFree,
                "creditBalance", wallet.getBalance()
        ));
    }

    @PostMapping("/usage/chat")
    public ResponseEntity<Map<String, Object>> consumeAIMessage(
            @JwtUser JwtClaims claims,
            @RequestBody Map<String, Object> body) {
        boolean canFree = aiChatCreditService.canSendFreeMessage(claims.getUserId());
        boolean success = aiChatCreditService.consumeMessage(claims.getUserId());
        int used = creditService.getDailyMessageCount(claims.getUserId());

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
            @JwtUser JwtClaims claims,
            @RequestParam Double amount) {
        return ResponseEntity.ok(creditService.rechargeBalance(claims.getUserId(), amount));
    }
}