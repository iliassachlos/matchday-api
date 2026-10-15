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
import jakarta.persistence.Version;
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

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "reserved_by")
    private UUID reservedBy;

    @Column(name = "reserved_until")
    private Instant reservedUntil;

    @Column(name = "order_id")
    private UUID orderId;

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

    /**
     * Check if the seat is available for reservation or sale.
     * 
     * @return true if the seat is available, false otherwise.
     */
    public boolean isAvailable() {
        return this.status == SeatStatus.AVAILABLE;
    }

    /**
     * Reserve the seat for a specific user until a given time.
     * 
     * @param userId - the ID of the user reserving the seat
     * @param until  - the time until which the seat is reserved
     */
    public void reserveFor(UUID userId, Instant until) {
        this.status = SeatStatus.RESERVED;
        this.reservedBy = userId;
        this.reservedUntil = until;
    }

    /**
     * Check if the seat is currently held by a specific user and has not expired.
     * 
     * @param userId - the ID of the user to check
     * @param now    - the current time to compare against the reservation
     *               expiration
     * @return true if the seat is held by the user and the hold has not expired,
     *         false otherwise.
     */
    public boolean isHeldBy(UUID userId, Instant now) {
        return this.status == SeatStatus.RESERVED
                && userId.equals(this.reservedBy)
                && this.reservedUntil.isAfter(now);
    }

    /**
     * Check if the seat is sold to a specific user and order.
     * 
     * @param userId  - the ID of the user to check
     * @param orderId - the ID of the order to check
     * @return true if the seat is sold to the user and order, false otherwise.
     */
    public boolean isSoldTo(UUID userId, UUID orderId) {
        return this.status == SeatStatus.SOLD
                && userId.equals(this.reservedBy)
                && orderId.equals(this.orderId);
    }

    /**
     * Sell the seat to a specific order, marking it as sold and clearing any
     * reservation.
     * 
     * @param orderId - the ID of the order to which the seat is sold
     */
    public void sellTo(UUID orderId) {
        this.status = SeatStatus.SOLD;
        this.reservedUntil = null;
        this.orderId = orderId;
    }

}
