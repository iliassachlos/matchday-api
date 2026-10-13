package com.matchday.ticket.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "seats")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Seat {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false)
    private String section;

    @Column(name = "row_label", nullable = false)
    private String rowLabel;

    @Column(name = "seat_number", nullable = false)
    private Integer number;

    @Column(name = "price_cents", nullable = false)
    private long priceCents;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SeatStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "reserved_by")
    private UUID reservedBy;

    public static Seat create(Event event, String section, String rowLabel, Integer number, long priceCents) {
        Seat seat = new Seat();

        seat.id = UUID.randomUUID();
        seat.event = event;
        seat.section = section;
        seat.rowLabel = rowLabel;
        seat.number = number;
        seat.priceCents = priceCents;
        seat.status = SeatStatus.AVAILABLE;
        seat.createdAt = Instant.now();

        return seat;
    }

    public boolean isAvailable() {
        return this.status == SeatStatus.AVAILABLE;
    }

    public void reserveFor(UUID userId) {
        this.status = SeatStatus.RESERVED;
        this.reservedBy = userId;
    }
}
