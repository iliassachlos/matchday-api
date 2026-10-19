package com.matchday.ticket.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.matchday.ticket.entity.Ticket;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    Optional<Ticket> findByOrderId(UUID orderId);

    /**
     * Marks a ticket as used if it is currently valid.
     * 
     * @param id  - The ID of the ticket to mark as used.
     * @param now - The current timestamp to set as the usedAt time.
     * @return The number of rows affected
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE Ticket t
            SET t.status = com.matchday.ticket.entity.TicketStatus.USED,
                t.usedAt = :now
            WHERE t.id = :id
              AND t.status = com.matchday.ticket.entity.TicketStatus.VALID
            """)
    int markUsed(@Param("id") UUID id, @Param("now") Instant now);
}
