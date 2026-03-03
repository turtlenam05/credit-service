package com.dathq.swd302.creditservice.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreditLockResult {
    private final boolean success;
    private final boolean freePost;
    private final int creditCost;       // 0 or 10
    private final String referenceId;
    private final String message;

    public static CreditLockResult free(String referenceId) {
        return CreditLockResult.builder()
                .success(true)
                .freePost(true)
                .creditCost(0)
                .referenceId(referenceId)
                .message("Bài đăng đầu tiên miễn phí!")
                .build();
    }

    public static CreditLockResult paid(String referenceId, int cost) {
        return CreditLockResult.builder()
                .success(true)
                .freePost(false)
                .creditCost(cost)
                .referenceId(referenceId)
                .message("Đã lock " + cost + " credit thành công.")
                .build();
    }

    public static CreditLockResult insufficient() {
        return CreditLockResult.builder()
                .success(false)
                .freePost(false)
                .creditCost(0)
                .message("Không đủ credit. Vui lòng nạp thêm để đăng bài.")
                .build();
    }
}
