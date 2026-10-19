package com.matchday.ticket.exception;

/**
 * The scanned code is forged, damaged, or points to no ticket. One exception for all
 * three, so a forger learns nothing about which part failed.
 */
public class TicketNotFoundException extends RuntimeException {

    public TicketNotFoundException() {
        super("Ticket not recognised");
    }
}
