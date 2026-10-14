package com.matchday.ticket.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.matchday.ticket.config.HoldProperties;
import com.matchday.ticket.dto.ReservationResponse;
import com.matchday.ticket.dto.SeatResponse;
import com.matchday.ticket.entity.Seat;
import com.matchday.ticket.exception.SeatNotAvailableException;
import com.matchday.ticket.exception.SeatNotFoundException;
import com.matchday.ticket.repository.SeatRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SeatReservationService {

    private final SeatRepository seatRepository;

    private final HoldProperties holdProperties;

    // Hold a seat for a user until the hold expires
    @Transactional
    public ReservationResponse reserve(UUID seatId, UUID userId) {
        Seat seat = seatRepository.findByIdForUpdate(seatId).orElseThrow(SeatNotFoundException::new);

        if (!seat.isAvailable()) {
            throw new SeatNotAvailableException();
        }

        Instant until = Instant.now().plus(holdProperties.duration());
        seat.reserveFor(userId, until);

        return new ReservationResponse(seatId, userId, seat.getStatus().name(), until);
    }

    // List all seats reserved by a user
    @Transactional(readOnly = true)
    public List<SeatResponse> listMySeats(UUID userId) {
        return seatRepository.findByReservedByOrderByNumberAsc(userId).stream()
                .map(SeatResponse::from)
                .toList();
    }

}
