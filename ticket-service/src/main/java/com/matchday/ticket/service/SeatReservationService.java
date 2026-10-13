package com.matchday.ticket.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.matchday.ticket.entity.Seat;
import com.matchday.ticket.exception.SeatNotAvailableException;
import com.matchday.ticket.exception.SeatNotFoundException;
import com.matchday.ticket.repository.SeatRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor
public class SeatReservationService {
    
    private final SeatRepository seatRepository;

    @Transactional 
    public void reserve(UUID seatId, UUID userId) {
        Seat seat = seatRepository.findByIdForUpdate(seatId).orElseThrow(SeatNotFoundException::new);

        if(!seat.isAvailable()) {
            throw new SeatNotAvailableException();
        }

        seat.reserveFor(userId);
    }
}
