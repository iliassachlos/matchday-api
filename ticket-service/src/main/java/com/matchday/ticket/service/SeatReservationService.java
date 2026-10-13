package com.matchday.ticket.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    // Reserve a seat for a user
    @Transactional
    public void reserve(UUID seatId, UUID userId) {
        Seat seat = seatRepository.findByIdForUpdate(seatId).orElseThrow(SeatNotFoundException::new);

        if (!seat.isAvailable()) {
            throw new SeatNotAvailableException();
        }

        seat.reserveFor(userId);
    }

    // List all seats reserved by a user
    @Transactional(readOnly = true)
    public List<SeatResponse> listMySeats(UUID userId) {
        return seatRepository.findByReservedByOrderByNumberAsc(userId).stream()
                .map(SeatResponse::from)
                .toList();
    }

}
