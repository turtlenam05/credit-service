package com.dathq.swd302.creditservice.controller;


import com.dathq.swd302.creditservice.dto.CreditLockResult;
import com.dathq.swd302.creditservice.dto.LockCreditRequest;
import com.dathq.swd302.creditservice.security.JwtClaims;
import com.dathq.swd302.creditservice.security.JwtUser;
import com.dathq.swd302.creditservice.service.IPostCreditService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/credits")
@RequiredArgsConstructor
public class PostCreditController {

    private final IPostCreditService postCreditService;

    @GetMapping("/posts/is-first")
    public ResponseEntity<Map<String, Object>> checkFirstPost(
            @JwtUser JwtClaims claims) {
        boolean isFirst = postCreditService.isFirstPost(claims.getUserId());
        return ResponseEntity.ok(Map.of(
                "isFirstPost", isFirst,
                "creditCost", isFirst ? 0 : 10,
                "message", isFirst ? "Bài đăng đầu tiên miễn phí!" : "Bài đăng này sẽ tốn 10 credit"
        ));
    }

    @PostMapping("/usage/post/lock")
    public ResponseEntity<Map<String, Object>> lockCreditForPost(
            @JwtUser JwtClaims claims,
            @RequestBody LockCreditRequest request) {
        CreditLockResult result = postCreditService.lockCreditForPost(claims.getUserId(), request.postId(), request.type());

        if (result.isSuccess()) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "isFreePost", result.isFreePost(),
                    "creditCost", result.getCreditCost(),
                    "referenceId", result.getReferenceId(), // caller stores this
                    "message", result.getMessage()
            ));
        }

        return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", result.getMessage()
        ));
    }

    @PostMapping("/usage/post/resolve")
    public ResponseEntity<Map<String, Object>> resolvePost(
            @JwtUser JwtClaims claims,
            @RequestBody Map<String, Object> body) {
        String transactionId = body.get("transactionId").toString();
        String action = body.get("action").toString();

        try {
            boolean result;
            String message;

            if ("APPROVE".equalsIgnoreCase(action)) {
                result = postCreditService.confirmPostApproved(claims.getUserId(), transactionId);
                message = "Đã trừ 10 credit chính thức. Bài đăng đã được publish.";
            } else if ("REJECT".equalsIgnoreCase(action)) {
                result = postCreditService.confirmPostRejected(claims.getUserId(), transactionId);
                message = "Đã hoàn 10 credit. Bài đăng bị từ chối.";
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "action không hợp lệ. Chỉ chấp nhận APPROVE hoặc REJECT."
                ));
            }

            return ResponseEntity.ok(Map.of("success", result, "message", message));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }
}
