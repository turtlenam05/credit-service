package com.dathq.swd302.creditservice.exception;

public class ReservationNotFoundException extends RuntimeException {
    public ReservationNotFoundException(String referenceId) {
        super("Reservation not found or already processed for referenceId: " + referenceId);
    }
}
