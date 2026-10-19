package com.matchday.ticket.service;

import java.time.Instant;
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
import com.matchday.ticket.exception.TicketAlreadyUsedException;
import com.matchday.ticket.exception.TicketNotFoundException;
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
    private final TicketCodeService ticketCodeService;

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

    /**
     * Admit the holder of a scanned QR code, at most once per ticket.
     *
     * @param code - the text read from the QR code
     * @return the ticket's seat details, for the steward to direct the holder
     * @throws TicketNotFoundException if the code is forged or points to no ticket
     * @throws TicketAlreadyUsedException if the ticket was already admitted
     */
    @Transactional
    public TicketDetails admit(String code) {
        UUID ticketId = ticketCodeService.verify(code).orElseThrow(TicketNotFoundException::new);

        if (ticketRepository.markUsed(ticketId, Instant.now()) == 0) {
            // Safe to read after the update: USED never goes back to VALID, so what we see stays true
            Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(TicketNotFoundException::new);
            throw new TicketAlreadyUsedException(ticket.getUsedAt());
        }

        log.info("Admitted ticket {}", ticketId);
        return details(ticketId);
    }

}
