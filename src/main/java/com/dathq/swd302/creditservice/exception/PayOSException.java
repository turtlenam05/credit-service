package com.dathq.swd302.creditservice.exception;

public class PayOSException extends RuntimeException {
    public PayOSException(String message) {
        super("PayOS API error: " + message);
    }

    public PayOSException(String message, Throwable cause) {
        super("PayOS API error: " + message, cause);
    }
}
