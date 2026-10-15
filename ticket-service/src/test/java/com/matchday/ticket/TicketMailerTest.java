package com.matchday.ticket;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClient;

import com.matchday.ticket.service.TicketMailerService;

class TicketMailerTest extends IntegrationTestBase {

    @Autowired
    private TicketMailerService mailer;

    @Value("${test.mailpit.url}")
    private String mailpitUrl;

    @Test
    @DisplayName("an email sent by the app really arrives in the mail server")
    void emailArrives() {
        String subject = "Your ticket " + UUID.randomUUID();

        mailer.send("maria@test.gr", subject, "See you at the match, Maria!");

        String inbox = RestClient.create(mailpitUrl).get()
                .uri("/api/v1/messages")
                .retrieve()
                .body(String.class);

        assertThat(inbox).contains(subject).contains("maria@test.gr");
    }
}
