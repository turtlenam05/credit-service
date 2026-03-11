package com.dathq.swd302.creditservice.exception;

import java.util.UUID;

public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException(UUID userId, int required, double available) {
        super(String.format("Insufficient balance for userId: %s. Required: %d, Available: %.2f",
                userId, required, available));
    }
}
