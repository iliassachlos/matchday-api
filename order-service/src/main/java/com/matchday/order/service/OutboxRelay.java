package com.matchday.order.service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.matchday.order.entity.OutboxMessage;
import com.matchday.order.repository.OutboxRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxRelay {

    private static final Duration SEND_TIMEOUT = Duration.ofSeconds(5);

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * Run every X seconds to send any waiting announcements to Kafka.
     */
    @Scheduled(fixedDelayString = "${matchday.outbox.relay-interval}")
    public void relay() {
        publishPending();
    }

    /**
     * Send every waiting announcement to Kafka, oldest first.
     * Stops at the first failure so the order is kept, the rest are retried on the
     * next run.
     *
     * @return How many announcements were sent
     */
    public int publishPending() {
        List<OutboxMessage> pending = outboxRepository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();
        int sent = 0;

        for (OutboxMessage message : pending) {
            try {
                // Wait for Kafka to confirm it stored the message before calling it sent
                kafkaTemplate.send(message.getTopic(), message.getMessageKey(), message.getPayload())
                        .get(SEND_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
                return sent;
            } catch (ExecutionException | TimeoutException _) {
                log.warn("Kafka unavailable; {} announcement(s) will be retried on the next run",
                        pending.size() - sent);
                return sent;
            }

            message.markPublished();
            outboxRepository.save(message);
            sent++;
        }

        if (sent > 0) {
            log.info("Relayed {} announcement(s) to Kafka", sent);
        }

        return sent;
    }
}
