package com.dathq.swd302.creditservice.controller;

import com.dathq.swd302.creditservice.entity.CreditTransaction;
import com.dathq.swd302.creditservice.entity.UserWallet;
import com.dathq.swd302.creditservice.service.CreditService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author matve
 */
@RestController
@RequestMapping("/api/credits")
@RequiredArgsConstructor
public class CreditController {

    private final CreditService creditService;

    // API nạp tiền thử nghiệm
    @PostMapping("/recharge")
    public ResponseEntity<UserWallet> recharge(
            @RequestParam Long userId,
            @RequestParam Double amount) {

        UserWallet updatedWallet = creditService.rechargeBalance(userId, amount);
        return ResponseEntity.ok(updatedWallet);
    }

    @GetMapping("/balance/{userId}")
    public ResponseEntity<UserWallet> getBalance(@PathVariable Long userId) {
        return ResponseEntity.ok(creditService.getWallet(userId));
    }

    @GetMapping("/history/{userId}")
    public ResponseEntity<List<CreditTransaction>> getHistory(@PathVariable Long userId) {
        return ResponseEntity.ok(creditService.getTransactionHistory(userId));
    }
}