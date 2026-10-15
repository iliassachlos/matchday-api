package com.matchday.ticket.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "venue_sections")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VenueSection {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    @Column(nullable = false)
    private String name;

    @Column(name = "row_count", nullable = false)
    private int rowCount;

    @Column(name = "seats_per_row", nullable = false)
    private int seatsPerRow;

    @Column(name = "price_cents", nullable = false)
    private long priceCents;

    public static VenueSection create(Venue venue, String name, int rowCount, int seatsPerRow, long priceCents) {
        VenueSection section = new VenueSection();

        section.id = UUID.randomUUID();
        section.venue = venue;
        section.name = name;
        section.rowCount = rowCount;
        section.seatsPerRow = seatsPerRow;
        section.priceCents = priceCents;

        return section;
    }

    /**
     * Calculate the total capacity of the venue section based on the number of rows
     * and seats per row.
     * 
     * @return
     */
    public int capacity() {
        return rowCount * seatsPerRow;
    }
}
