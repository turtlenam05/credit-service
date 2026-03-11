package com.dathq.swd302.creditservice.exception;

public class ReservationAlreadyExistsException extends RuntimeException {
    public ReservationAlreadyExistsException(String referenceId) {
        super("Reservation already exists for referenceId: " + referenceId);
    }
}
