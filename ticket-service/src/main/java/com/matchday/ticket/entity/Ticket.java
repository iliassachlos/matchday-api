package com.matchday.ticket.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tickets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ticket {
    @Id
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "seat_id", nullable = false)
    private UUID seatId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "holder_email", nullable = false)
    private String holderEmail;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TicketStatus status;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "emailed_at")
    private Instant emailedAt;

    public static Ticket issue(UUID orderId, UUID seatId, UUID userId, String holderEmail) {
        Ticket ticket = new Ticket();

        ticket.id = UUID.randomUUID();
        ticket.orderId = orderId;
        ticket.seatId = seatId;
        ticket.userId = userId;
        ticket.holderEmail = holderEmail;
        ticket.status = TicketStatus.VALID;
        ticket.issuedAt = Instant.now();

        return ticket;
    }

    /**
     * Marks the ticket as emailed by setting the emailedAt timestamp to the current
     * time.
     */
    public void markEmailed() {
        this.emailedAt = Instant.now();
    }

}
