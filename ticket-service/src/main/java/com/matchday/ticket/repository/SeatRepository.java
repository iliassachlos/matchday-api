package com.matchday.ticket.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.matchday.ticket.entity.Seat;

import jakarta.persistence.LockModeType;

public interface SeatRepository extends JpaRepository<Seat, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT seat FROM Seat seat WHERE seat.id = :id")
    Optional<Seat> findByIdForUpdate(@Param("id") UUID id);

    List<Seat> findByEventIdOrderByNumberAsc(UUID eventId);

    List<Seat> findByReservedByOrderByNumberAsc(UUID reservedBy);

}
