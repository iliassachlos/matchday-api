package com.matchday.ticket.exception;

public class SeatNotHeldException extends RuntimeException {

    public SeatNotHeldException() {
        super("Seat is not held by you, or your hold has expired");
    }
}
