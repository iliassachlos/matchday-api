package com.matchday.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.ConsumerFactory;

import com.matchday.order.entity.OutboxMessage;
import com.matchday.order.repository.OutboxRepository;
import com.matchday.order.service.OrderOutboxService;
import com.matchday.order.service.OutboxRelay;

class OutboxRelayTest extends IntegrationTestBase {

    @Autowired
    private OutboxRelay relay;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private ConsumerFactory<String, String> consumerFactory;

    @Test
    @DisplayName("a waiting announcement reaches Kafka and is then marked as sent")
    void announcementReachesKafka() {
        String key = UUID.randomUUID().toString();
        OutboxMessage message = outboxRepository.save(
                OutboxMessage.create(OrderOutboxService.ORDER_PAID_TOPIC, key, "{\"orderId\":\"" + key + "\"}"));

        relay.publishPending();

        assertThat(readFromKafka(OrderOutboxService.ORDER_PAID_TOPIC, key)).isTrue();
        assertThat(outboxRepository.findById(message.getId()).orElseThrow().getPublishedAt()).isNotNull();
    }

    // Reads the topic from the beginning, the way ticket-service will, until the key shows up or 10 seconds pass
    private boolean readFromKafka(String topic, String key) {
        Properties fromTheStart = new Properties();
        fromTheStart.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        try (Consumer<String, String> consumer =
                consumerFactory.createConsumer("outbox-test-" + key, null, null, fromTheStart)) {
            consumer.subscribe(List.of(topic));

            List<ConsumerRecord<String, String>> received = new ArrayList<>();
            long deadline = System.currentTimeMillis() + 10_000;

            while (System.currentTimeMillis() < deadline) {
                consumer.poll(Duration.ofMillis(500)).forEach(received::add);
                if (received.stream().anyMatch(record -> key.equals(record.key()))) {
                    return true;
                }
            }
            return false;
        }
    }
}
