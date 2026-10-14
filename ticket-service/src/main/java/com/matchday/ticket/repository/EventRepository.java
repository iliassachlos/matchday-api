package com.matchday.ticket.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.matchday.ticket.entity.Event;

public interface EventRepository extends JpaRepository<Event, UUID> {

    @Query("SELECT e FROM Event e JOIN FETCH e.venue ORDER BY e.startsAt")
    List<Event> findAllWithVenue();
}
