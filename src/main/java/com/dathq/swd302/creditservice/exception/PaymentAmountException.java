package com.dathq.swd302.creditservice.exception;

public class PaymentAmountException extends RuntimeException {
    public PaymentAmountException(int amount, int min, int max) {
        super(String.format(
                "Payment amount %d is out of range. Must be between %d and %d VNĐ.",
                amount, min, max
        ));
    }
}
