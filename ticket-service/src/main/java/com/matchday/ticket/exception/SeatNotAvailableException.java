package com.matchday.ticket.exception;

public class SeatNotAvailableException extends RuntimeException {

    public SeatNotAvailableException() {
        super("Seat is not available");
    }
}
