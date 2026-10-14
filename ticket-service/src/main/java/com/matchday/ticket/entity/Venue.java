package com.matchday.ticket.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "venues")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Venue {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String city;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static Venue create(String name, String city) {
        Venue venue = new Venue();

        venue.id = UUID.randomUUID();
        venue.name = name;
        venue.city = city;
        venue.createdAt = Instant.now();

        return venue;
    }
}
