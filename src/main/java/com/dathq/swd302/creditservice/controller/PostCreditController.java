package com.dathq.swd302.creditservice.controller;


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
            @RequestHeader("X-User-Id") UUID userId) {
        boolean isFirst = postCreditService.isFirstPost(userId);
        return ResponseEntity.ok(Map.of(
                "isFirstPost", isFirst,
                "creditCost", isFirst ? 0 : 10,
                "message", isFirst ? "Bài đăng đầu tiên miễn phí!" : "Bài đăng này sẽ tốn 10 credit"
        ));
    }

    @PostMapping("/usage/post/lock")
    public ResponseEntity<Map<String, Object>> lockCreditForPost(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody Map<String, Object> body) {
        String postId = body.get("postId").toString();
        boolean locked = postCreditService.lockCreditForPost(userId, postId);

        if (locked) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã lock 10 credit thành công. Bài đăng đang chờ duyệt."
            ));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Không đủ credit. Vui lòng nạp thêm để đăng bài."
            ));
        }
    }

    @PostMapping("/usage/post/resolve")
    public ResponseEntity<Map<String, Object>> resolvePost(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody Map<String, Object> body) {
        String transactionId = body.get("transactionId").toString();
        String action = body.get("action").toString();

        try {
            boolean result;
            String message;

            if ("APPROVE".equalsIgnoreCase(action)) {
                result = postCreditService.confirmPostApproved(userId, transactionId);
                message = "Đã trừ 10 credit chính thức. Bài đăng đã được publish.";
            } else if ("REJECT".equalsIgnoreCase(action)) {
                result = postCreditService.confirmPostRejected(userId, transactionId);
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
