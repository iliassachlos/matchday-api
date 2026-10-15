package com.matchday.order.exception;

public class TicketServiceUnavailableException extends RuntimeException {

    public TicketServiceUnavailableException(String message) {
        super(message);
    }

    public TicketServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}