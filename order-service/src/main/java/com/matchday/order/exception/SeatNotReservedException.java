package com.matchday.order.exception;

public class SeatNotReservedException extends RuntimeException {

    public SeatNotReservedException() {
        super("Reserve the seat before paying for it");
    }
}
