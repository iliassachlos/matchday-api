package com.matchday.ticket.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.matchday.ticket.dto.EventAvailability;
import com.matchday.ticket.entity.Seat;
import com.matchday.ticket.entity.SeatStatus;

import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;

public interface SeatRepository extends JpaRepository<Seat, UUID> {

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT seat FROM Seat seat WHERE seat.id = :id")
        Optional<Seat> findByIdForUpdate(@Param("id") UUID id);

        @Query("""
                        SELECT s FROM Seat s
                        WHERE s.event.id = :eventId
                        ORDER BY s.section, LENGTH(s.rowLabel), s.rowLabel, s.number
                        """)
        List<Seat> findSeatMap(@Param("eventId") UUID eventId);

        List<Seat> findByReservedByOrderByNumberAsc(UUID reservedBy);

        @Query("""
                        SELECT new com.matchday.ticket.dto.EventAvailability(s.event.id, COUNT(s))
                        FROM Seat s
                        WHERE s.status = :status
                        GROUP BY s.event.id
                        """)
        List<EventAvailability> countByEventWithStatus(@Param("status") SeatStatus status);

        @Transactional
        @Modifying
        @Query("""
                        UPDATE Seat s
                        SET s.status = SeatStatus.AVAILABLE,
                            s.reservedBy = null,
                            s.reservedUntil = null,
                            s.version = s.version + 1
                        WHERE s.status = com.matchday.ticket.entity.SeatStatus.RESERVED
                          AND s.reservedUntil < :now
                        """)
        int releaseExpiredHolds(@Param("now") Instant now);

}
