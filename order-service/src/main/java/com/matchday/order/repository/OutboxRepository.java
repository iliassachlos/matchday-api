package com.matchday.order.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.matchday.order.entity.OutboxMessage;

public interface OutboxRepository extends JpaRepository<OutboxMessage, UUID> {

    List<OutboxMessage> findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();

}
