package com.matchday.ticket.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.matchday.ticket.dto.OrderPaidEvent;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class OrderPaidListenerService {

    public static final String ORDER_PAID_TOPIC = "orders.paid";

    private final ObjectMapper objectMapper;
    private final TicketService ticketService;

    @KafkaListener(topics = ORDER_PAID_TOPIC)
    public void onOrderPaid(String payload) {
        OrderPaidEvent event = objectMapper.readValue(payload, OrderPaidEvent.class);
        
        ticketService.issue(event);
    }
}
