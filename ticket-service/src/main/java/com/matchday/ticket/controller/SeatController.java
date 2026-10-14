package com.matchday.ticket.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.matchday.ticket.service.SeatReservationService;
import com.matchday.ticket.dto.ReservationResponse;
import com.matchday.ticket.dto.SeatResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/seats")
@RequiredArgsConstructor
public class SeatController {

    private final SeatReservationService seatReservationService;

    @PostMapping("/{seatId}/reserve")
    public ResponseEntity<ReservationResponse> reserve(@PathVariable UUID seatId, @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());

        return ResponseEntity.ok(seatReservationService.reserve(seatId, userId));
    }

    @GetMapping("/me")
    public List<SeatResponse> listMySeats(@AuthenticationPrincipal Jwt jwt) {
        return seatReservationService.listMySeats(UUID.fromString(jwt.getSubject()));
    }

}
