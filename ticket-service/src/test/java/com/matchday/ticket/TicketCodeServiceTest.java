package com.matchday.ticket;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.matchday.ticket.service.TicketCodeService;

// Plain unit test: no Spring context, no database
class TicketCodeServiceTest {

    private final TicketCodeService codes = new TicketCodeService("test-secret");

    @Test
    @DisplayName("a code made by ticket-service is accepted and gives back the ticket id")
    void genuineCodeIsAccepted() {
        UUID ticketId = UUID.randomUUID();

        assertThat(codes.verify(codes.codeFor(ticketId))).contains(ticketId);
    }

    @Test
    @DisplayName("swapping in another ticket's id breaks the signature")
    void tamperedCodeIsRejected() {
        String genuine = codes.codeFor(UUID.randomUUID());
        String signature = genuine.substring(genuine.lastIndexOf('.') + 1);

        String forged = UUID.randomUUID() + "." + signature;

        assertThat(codes.verify(forged)).isEmpty();
    }

    @Test
    @DisplayName("a code signed with a different secret is rejected")
    void codeFromAnotherSecretIsRejected() {
        String foreign = new TicketCodeService("someone-elses-secret").codeFor(UUID.randomUUID());

        assertThat(codes.verify(foreign)).isEmpty();
    }

    @Test
    @DisplayName("text that is not a ticket code at all is rejected, not an error")
    void garbageIsRejected() {
        assertThat(codes.verify("hello")).isEmpty();
        assertThat(codes.verify("not-a-uuid.abc")).isEmpty();
    }
}
