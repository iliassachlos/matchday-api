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

    // Scheduled task to release expired holds at a fixed interval
    @Scheduled(fixedDelayString = "${matchday.hold.release-interval}")
    public void releaseExpiredHolds() {
        releaseExpiredHolds(Instant.now());
    }

    // Release expired holds and log the number of released holds
    public int releaseExpiredHolds(Instant now) {
        int released = seatRepository.releaseExpiredHolds(now);

        if (released > 0) {
            log.info("Released {} expired seat holds", released);
        }

        return released;
    }
}
