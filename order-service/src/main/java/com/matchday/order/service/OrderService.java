package com.matchday.order.service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.matchday.order.dto.OrderResponse;
import com.matchday.order.dto.PaymentResult;
import com.matchday.order.dto.SaleOutcome;
import com.matchday.order.dto.SeatInfo;
import com.matchday.order.entity.Order;
import com.matchday.order.exception.SeatNotReservedException;
import com.matchday.order.exception.TicketServiceUnavailableException;
import com.matchday.order.repository.OrderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final int SELL_ATTEMPTS = 3;
    private static final Duration FIRST_RETRY_DELAY = Duration.ofMillis(200);

    private final OrderRepository orderRepository;
    private final PaymentService paymentService;
    private final TicketClientService ticketClient;

    /**
     * Place an order for a seat, charging the user and selling the seat in
     * ticket-service.
     * 
     * @param userId        - User ID placing the order
     * @param seatId        - Seat ID to order
     * @param paymentMethod - Payment method to charge
     * @param bearerToken   - Bearer token to authenticate with ticket-service
     * @return Outcome of the order
     */
    public OrderResponse placeOrder(UUID userId, UUID seatId, String paymentMethod, String bearerToken) {
        SeatInfo seat = ticketClient.getSeat(seatId, bearerToken);

        // Cheap early exit so nobody is charged for a seat they never reserved.
        if (!"RESERVED".equals(seat.status())) {
            throw new SeatNotReservedException();
        }

        Order order = orderRepository.save(Order.create(userId, seatId, seat.priceCents()));
        PaymentResult payment = paymentService.charge(order.getId(), order.getAmountCents(), paymentMethod);

        if (!payment.approved()) {
            order.markFailed();
            return OrderResponse.from(orderRepository.save(order));
        }

        order.recordCharge(payment.reference());
        order = orderRepository.save(order);

        if (sellSeat(seatId, order.getId(), bearerToken) == SaleOutcome.SOLD) {
            order.markPaid();
        } else {
            paymentService.refund(payment.reference(), order.getAmountCents());
            order.markRefunded();
        }

        return OrderResponse.from(orderRepository.save(order));
    }

    /**
     * List all orders for a user, most recent first.
     * 
     * @param userId - User ID to list orders for
     * @return List of orders for the user
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> listMyOrders(UUID userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(OrderResponse::from)
                .toList();
    }

    /**
     * Sell a seat in ticket-service
     * 
     * @param seatId      - Seat ID to sell
     * @param orderId     - Order ID to associate with the sale
     * @param bearerToken - Bearer token to authenticate with ticket-service
     * @return Outcome of the sale
     */
    private SaleOutcome sellSeat(UUID seatId, UUID orderId, String bearerToken) {
        Duration delay = FIRST_RETRY_DELAY;

        for (int attempt = 1;; attempt++) {
            try {
                return ticketClient.sell(seatId, orderId, bearerToken);
            } catch (TicketServiceUnavailableException e) {
                if (attempt == SELL_ATTEMPTS) {
                    log.error("Sale of seat {} for order {} still unconfirmed after {} attempts; refunding",
                            seatId, orderId, attempt, e);
                    return SaleOutcome.REJECTED;
                }

                log.warn("Sale of seat {} for order {} unconfirmed (attempt {}), retrying", seatId, orderId, attempt);
                pause(delay);
                delay = delay.multipliedBy(2);
            }
        }
    }

    /**
     * Pause the current thread for a given duration
     * 
     * @param delay - Duration to pause
     */
    private void pause(Duration delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TicketServiceUnavailableException("Interrupted while retrying the sale", e);
        }
    }
}
