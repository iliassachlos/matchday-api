package com.matchday.ticket.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.matchday.ticket.entity.Event;

public interface EventRepository extends JpaRepository<Event, UUID> {
}
