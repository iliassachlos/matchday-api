package com.matchday.order.entity;

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
@Table(name = "outbox")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxMessage {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String topic;

    @Column(name = "message_key", nullable = false)
    private String messageKey;

    @Column(nullable = false)
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    public static OutboxMessage create(String topic, String messageKey, String payload) {
        OutboxMessage message = new OutboxMessage();

        message.id = UUID.randomUUID();
        message.topic = topic;
        message.messageKey = messageKey;
        message.payload = payload;
        message.createdAt = Instant.now();

        return message;
    }

    /**
     * Marks the message as published
     */
    public void markPublished() {
        this.publishedAt = Instant.now();
    }
}
