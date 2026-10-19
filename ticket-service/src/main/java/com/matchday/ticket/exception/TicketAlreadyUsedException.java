package com.matchday.ticket.exception;

import java.time.Instant;

import lombok.Getter;

@Getter
public class TicketAlreadyUsedException extends RuntimeException {

    private final Instant usedAt;

    public TicketAlreadyUsedException(Instant usedAt) {
        super("Ticket was already used at " + usedAt);
        this.usedAt = usedAt;
    }
}
