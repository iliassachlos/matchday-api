package com.matchday.ticket.service;

import java.time.Instant;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.matchday.ticket.repository.SeatRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeatHoldReleaser {

    private final SeatRepository seatRepository;

    /**
     * Release expired holds at a fixed interval defined in the application
     * properties.
     */
    @Scheduled(fixedDelayString = "${matchday.hold.release-interval}")
    public void releaseExpiredHolds() {
        releaseExpiredHolds(Instant.now());
    }

    /**
     * Release expired holds at a specific point in time. This method is useful for
     * testing purposes.
     * 
     * @param now - The point in time to check for expired holds
     * @return The number of expired holds released
     */
    public int releaseExpiredHolds(Instant now) {
        int released = seatRepository.releaseExpiredHolds(now);

        if (released > 0) {
            log.info("Released {} expired seat holds", released);
        }

        return released;
    }
}
