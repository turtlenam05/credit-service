package com.dathq.swd302.creditservice.controller;


import com.dathq.swd302.creditservice.dto.RefundRequestDTO;
import com.dathq.swd302.creditservice.entity.RefundRequest;
import com.dathq.swd302.creditservice.security.JwtClaims;
import com.dathq.swd302.creditservice.security.JwtUser;
import com.dathq.swd302.creditservice.service.IRefundRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/credits/refunds")
@RequiredArgsConstructor
public class RefundRequestController {

    private final IRefundRequestService refundRequestService;

    @PostMapping
    public ResponseEntity<?> submitRefund(
            @JwtUser JwtClaims claims,
            @RequestBody RefundRequestDTO dto) {
        try {
            RefundRequest request = refundRequestService.submitRefundRequest(claims.getUserId(), dto);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "requestId", request.getId(),
                    "message", "Refund Request đã được gửi. Admin sẽ xem xét trong thời gian sớm nhất."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<RefundRequest>> getUserRefunds(
            @JwtUser JwtClaims claims) {
        return ResponseEntity.ok(refundRequestService.getRefundRequestsByUser(claims.getUserId()));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<RefundRequest>> getPendingRequests(
            @JwtUser JwtClaims claims) {
        return ResponseEntity.ok(refundRequestService.getAllPendingRequests());
    }

    @PostMapping("/{requestId}/approve")
    public ResponseEntity<?> approveRefund(
            @JwtUser JwtClaims claims,
            @PathVariable Long requestId) {
        try {
            RefundRequest result = refundRequestService.approveRefundRequest(requestId, claims.getUserId());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "status", result.getStatus(),
                    "message", "Đã duyệt hoàn credit thành công."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/{requestId}/reject")
    public ResponseEntity<?> rejectRefund(
            @JwtUser JwtClaims claims,
            @PathVariable Long requestId,
            @RequestBody Map<String, String> body) {
        try {
            String reason = body.get("reason");
            RefundRequest result = refundRequestService.rejectRefundRequest(requestId, claims.getUserId(), reason);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "status", result.getStatus(),
                    "message", "Đã từ chối Refund Request."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }
}
