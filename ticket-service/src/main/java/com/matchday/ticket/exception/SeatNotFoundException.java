package com.matchday.ticket.exception;

public class SeatNotFoundException extends RuntimeException {

    public SeatNotFoundException() {
        super("Seat not found");
    }
}
