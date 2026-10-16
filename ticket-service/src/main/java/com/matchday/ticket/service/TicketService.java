package com.matchday.ticket.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.matchday.ticket.dto.OrderPaidEvent;
import com.matchday.ticket.dto.TicketDetails;
import com.matchday.ticket.entity.Seat;
import com.matchday.ticket.entity.SeatStatus;
import com.matchday.ticket.entity.Ticket;
import com.matchday.ticket.exception.SeatNotFoundException;
import com.matchday.ticket.repository.SeatRepository;
import com.matchday.ticket.repository.TicketRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final SeatRepository seatRepository;

    /**
     * Issue a ticket for the given order, if it has not already been issued.
     *
     * @param event - the "order paid" announcement from order-service
     * @return the ticket, new or already existing
     */
    @Transactional
    public Ticket issue(OrderPaidEvent event) {
        Optional<Ticket> existing = ticketRepository.findByOrderId(event.orderId());
        if (existing.isPresent()) {
            log.info("Ticket for order {} already issued; ignoring the repeated announcement", event.orderId());
            return existing.get();
        }

        // Validate that the seat is actually sold to this order, and issue the ticket
        Seat seat = seatRepository.findById(event.seatId()).orElseThrow(SeatNotFoundException::new);
        if (seat.getStatus() != SeatStatus.SOLD || !event.orderId().equals(seat.getOrderId())) {
            throw new IllegalStateException(
                    "Order " + event.orderId() + " is announced as paid, but seat " + seat.getId()
                            + " was not sold to it");
        }

        Ticket ticket = ticketRepository.save(
                Ticket.issue(event.orderId(), seat.getId(), event.userId(), event.buyerEmail()));

        log.info("Issued ticket {} for order {}", ticket.getId(), event.orderId());
        return ticket;
    }

    /**
     * Returns the details of the ticket with the given ID.
     * 
     * @param ticketId - the ID of the ticket
     * @return the details of the ticket
     */
    @Transactional(readOnly = true)
    public TicketDetails details(UUID ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow();
        Seat seat = seatRepository.findById(ticket.getSeatId()).orElseThrow(SeatNotFoundException::new);

        return new TicketDetails(
                ticket.getId(),
                ticket.getHolderEmail(),
                seat.getEvent().getName(),
                seat.getEvent().getVenue().getName(),
                seat.getEvent().getStartsAt(),
                seat.getSection(),
                seat.getRowLabel(),
                seat.getNumber());
    }

    /**
     * Marks the ticket with the given ID as having been emailed to the holder.
     * 
     * @param ticketId - the ID of the ticket
     */
    @Transactional
    public void markEmailed(UUID ticketId) {
        ticketRepository.findById(ticketId).orElseThrow().markEmailed();
    }

}
