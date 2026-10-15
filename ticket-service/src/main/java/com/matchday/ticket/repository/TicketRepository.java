package com.matchday.ticket.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.matchday.ticket.entity.Ticket;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    Optional<Ticket> findByOrderId(UUID orderId);
}
