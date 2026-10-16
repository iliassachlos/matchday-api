package com.matchday.ticket.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.matchday.ticket.dto.OrderPaidEvent;
import com.matchday.ticket.entity.Ticket;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class OrderPaidListenerService {

    public static final String ORDER_PAID_TOPIC = "orders.paid";

    private final TicketService ticketService;
    private final TicketEmailService ticketEmailService;

    private final ObjectMapper objectMapper;

    @KafkaListener(topics = ORDER_PAID_TOPIC)
    public void onOrderPaid(String payload) {
        OrderPaidEvent event = objectMapper.readValue(payload, OrderPaidEvent.class);
        Ticket ticket = ticketService.issue(event);

        // Send the ticket email if it has not already been sent
        if (ticket.getEmailedAt() == null) {
            ticketEmailService.send(ticket.getId());
        }
    }

}
