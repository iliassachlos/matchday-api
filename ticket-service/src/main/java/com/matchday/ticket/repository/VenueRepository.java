package com.matchday.ticket.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.matchday.ticket.entity.Venue;

public interface VenueRepository extends JpaRepository<Venue, UUID>{
    
}
