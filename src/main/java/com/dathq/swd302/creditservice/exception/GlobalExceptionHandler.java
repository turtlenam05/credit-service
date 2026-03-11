package com.dathq.swd302.creditservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ─── 404 Not Found ────────────────────────────────────────────────────────

    @ExceptionHandler(WalletNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleWalletNotFound(
            WalletNotFoundException ex, HttpServletRequest request) {
        log.warn("Wallet not found: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, "Wallet Not Found", ex.getMessage(), request);
    }

    @ExceptionHandler(ReservationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleReservationNotFound(
            ReservationNotFoundException ex, HttpServletRequest request) {
        log.warn("Reservation not found: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, "Reservation Not Found", ex.getMessage(), request);
    }

    // ─── 409 Conflict ─────────────────────────────────────────────────────────

    @ExceptionHandler(ReservationAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleReservationAlreadyExists(
            ReservationAlreadyExistsException ex, HttpServletRequest request) {
        log.warn("Reservation conflict: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, "Reservation Conflict", ex.getMessage(), request);
    }

    // ─── 402 Payment Required ─────────────────────────────────────────────────

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientBalance(
            InsufficientBalanceException ex, HttpServletRequest request) {
        log.warn("Insufficient balance: {}", ex.getMessage());
        return build(HttpStatus.PAYMENT_REQUIRED, "Insufficient Balance", ex.getMessage(), request);
    }

    // ─── 422 Payment Amount Out Of Range ──────────────────────────────────────

    @ExceptionHandler(PaymentAmountException.class)
    public ResponseEntity<ErrorResponse> handlePaymentAmount(
            PaymentAmountException ex, HttpServletRequest request) {
        log.warn("Invalid payment amount: {}", ex.getMessage());
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid Payment Amount", ex.getMessage(), request);
    }

    // ─── 502 Bad Gateway (external PayOS failure) ─────────────────────────────

    @ExceptionHandler(PayOSException.class)
    public ResponseEntity<ErrorResponse> handlePayOS(
            PayOSException ex, HttpServletRequest request) {
        log.error("PayOS upstream error: {}", ex.getMessage());
        return build(HttpStatus.BAD_GATEWAY, "Payment Gateway Error", ex.getMessage(), request);
    }

    // ─── 400 Validation ───────────────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
                        (a, b) -> a
                ));

        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("One or more fields are invalid")
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .validationErrors(fieldErrors)
                .build();

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request);
    }

    // ─── 500 Internal Server Error ────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected error at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred. Please try again later.", request);
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private ResponseEntity<ErrorResponse> build(
            HttpStatus status, String error, String message, HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.builder()
                .status(status.value())
                .error(error)
                .message(message)
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(status).body(body);
    }
}