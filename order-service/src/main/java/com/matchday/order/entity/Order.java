package com.matchday.order.entity;

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
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {
    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "buyer_email")
    private String buyerEmail;

    @Column(name = "seat_id", nullable = false)
    private UUID seatId;

    @Column(name = "amount_cents", nullable = false)
    private long amountCents;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Column(name = "payment_reference")
    private String paymentReference;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static Order create(UUID userId, String buyerEmail, UUID seatId, long amountCents) {
        Order order = new Order();

        order.id = UUID.randomUUID();
        order.userId = userId;
        order.buyerEmail = buyerEmail;
        order.seatId = seatId;
        order.amountCents = amountCents;
        order.status = OrderStatus.PENDING;
        order.createdAt = Instant.now();
        order.updatedAt = order.createdAt;

        return order;
    }

    /**
     * Record the payment reference for this order. This method can only be called
     * when the order is in the PENDING state.
     * 
     * @param paymentReference - The payment reference
     */
    public void recordCharge(String paymentReference) {
        requirePending();
        this.paymentReference = paymentReference;
        this.updatedAt = Instant.now();
    }

    /**
     * Mark the order as paid.
     */
    public void markPaid() {
        moveTo(OrderStatus.PAID);
    }

    /**
     * Mark the order as failed.
     */
    public void markFailed() {
        moveTo(OrderStatus.FAILED);
    }

    /**
     * Mark the order as refunded.
     */
    public void markRefunded() {
        moveTo(OrderStatus.REFUNDED);
    }

    /**
     * Move the order to a new status. This method can only be called when the order
     * is in the PENDING state.
     * 
     * @param newStatus
     */
    private void moveTo(OrderStatus newStatus) {
        requirePending();

        this.status = newStatus;
        this.updatedAt = Instant.now();
    }

    /**
     * Helper method to ensure that the order is in the PENDING state before
     * allowing certain operation.
     */
    private void requirePending() {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException("Order " + id + " is already " + status);
        }
    }
}
