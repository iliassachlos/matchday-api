package com.matchday.order.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.matchday.order.dto.OrderPaidEvent;
import com.matchday.order.entity.Order;
import com.matchday.order.entity.OutboxMessage;
import com.matchday.order.repository.OrderRepository;
import com.matchday.order.repository.OutboxRepository;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class OrderOutboxService {

    public static final String ORDER_PAID_TOPIC = "orders.paid";

    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    /**
     * Mark the order as paid and record the announcement, in one transaction:
     * either both are saved or neither is, so a paid order can never be left without its ticket.
     *
     * @param order - the order whose seat ticket-service just sold
     * @return the saved order
     */
    @Transactional
    public Order markPaidAndAnnounce(Order order) {
        order.markPaid();
        
        Order saved = orderRepository.save(order);

        OrderPaidEvent event = new OrderPaidEvent(
                saved.getId(),
                saved.getUserId(),
                saved.getBuyerEmail(),
                saved.getSeatId(),
                saved.getAmountCents(),
                Instant.now());

        outboxRepository.save(OutboxMessage.create(
                ORDER_PAID_TOPIC, saved.getId().toString(), objectMapper.writeValueAsString(event)));

        return saved;
    }
}
