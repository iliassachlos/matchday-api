package com.matchday.ticket.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.matchday.ticket.config.HoldProperties;
import com.matchday.ticket.dto.ReservationResponse;
import com.matchday.ticket.dto.SaleResponse;
import com.matchday.ticket.dto.SeatResponse;
import com.matchday.ticket.entity.Seat;
import com.matchday.ticket.exception.SeatNotAvailableException;
import com.matchday.ticket.exception.SeatNotFoundException;
import com.matchday.ticket.exception.SeatNotHeldException;
import com.matchday.ticket.repository.SeatRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SeatReservationService {

    private final SeatRepository seatRepository;

    private final HoldProperties holdProperties;

    /**
     * Reserve a seat for a user. The seat will be held for a certain duration,
     * after which it will be released if not purchased.
     * 
     * @param seatId - The ID of the seat to reserve
     * @param userId - The ID of the user reserving the seat
     * @return ReservationResponse containing the reservation details
     */
    @Transactional
    public ReservationResponse reserve(UUID seatId, UUID userId) {
        Seat seat = seatRepository.findByIdForUpdate(seatId).orElseThrow(SeatNotFoundException::new);

        // A retry of a reservation that already succeeded (e.g. the response was lost):
        // (seat, user) is the idempotency key. Return the same hold, not extended, so
        // retrying can never keep a seat held forever.
        if (seat.isHeldBy(userId, Instant.now())) {
            return new ReservationResponse(seatId, userId, seat.getStatus().name(), seat.getReservedUntil());
        }

        if (!seat.isAvailable()) {
            throw new SeatNotAvailableException();
        }

        // Postgres keeps microseconds; truncate so the response matches what is stored
        Instant until = Instant.now().plus(holdProperties.duration()).truncatedTo(ChronoUnit.MICROS);
        seat.reserveFor(userId, until);

        return new ReservationResponse(seatId, userId, seat.getStatus().name(), until);
    }

    /**
     * List all seats reserved by a specific user, ordered by seat number.
     * 
     * @param userId - The ID of the user whose reserved seats are to be listed
     * @return List of SeatResponse objects representing the reserved seats
     */
    @Transactional(readOnly = true)
    public List<SeatResponse> listMySeats(UUID userId) {
        return seatRepository.findByReservedByOrderByNumberAsc(userId).stream()
                .map(SeatResponse::from)
                .toList();
    }

    /**
     * Sell a reserved seat to a user. The seat must be held by the user and not
     * already sold.
     * 
     * @param seatId  - The ID of the seat to sell
     * @param userId  - The ID of the user purchasing the seat
     * @param orderId - The ID of the order associated with the sale
     * @return SaleResponse containing the sale details
     */
    @Transactional
    public SaleResponse sell(UUID seatId, UUID userId, UUID orderId) {
        Seat seat = seatRepository.findByIdForUpdate(seatId).orElseThrow(SeatNotFoundException::new);

        if (seat.isSoldTo(userId, orderId)) {
            return SaleResponse.from(seat);
        }

        if (!seat.isHeldBy(userId, Instant.now())) {
            throw new SeatNotHeldException();
        }

        seat.sellTo(orderId);

        return SaleResponse.from(seat);
    }

    /**
     * Retrieve the details of a specific seat by its ID
     * 
     * @param seatId - The ID of the seat to retrieve
     * @return SeatResponse containing the seat details
     */
    @Transactional
    public SeatResponse getSeat(UUID seatId) {
        Seat seat = seatRepository.findById(seatId).orElseThrow(SeatNotFoundException::new);

        return SeatResponse.from(seat);
    }
}
