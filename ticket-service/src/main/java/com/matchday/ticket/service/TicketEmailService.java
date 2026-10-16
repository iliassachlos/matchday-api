package com.matchday.ticket.service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import com.matchday.ticket.dto.TicketDetails;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketEmailService {

    private static final DateTimeFormatter KICK_OFF = DateTimeFormatter
            .ofPattern("EEEE d MMMM yyyy, HH:mm", Locale.ENGLISH)
            .withZone(ZoneId.of("Europe/Athens"));

    private final TicketService ticketService;
    private final TicketCodeService ticketCodeService;
    private final QrCodeService qrCodeService;
    private final TicketMailerService mailer;

    /**
     * Sends the ticket email to the holder of the ticket with the given ID.
     * 
     * @param ticketId - the ID of the ticket
     */
    public void send(UUID ticketId) {
        TicketDetails ticket = ticketService.details(ticketId);
        byte[] qr = qrCodeService.png(ticketCodeService.codeFor(ticketId));

        mailer.sendTicket(ticket.holderEmail(), "Your ticket: " + ticket.matchName(), html(ticket), qr);
        ticketService.markEmailed(ticketId);

        log.info("Emailed ticket {} to {}", ticketId, ticket.holderEmail());
    }

    /**
     * Returns the HTML body of the ticket email for the given ticket details.
     * 
     * @param ticket - the details of the ticket
     * @return the HTML body of the ticket email
     */
    private String html(TicketDetails ticket) {
        return """
                <h2>%s</h2>
                <p>%s<br>%s</p>
                <p><b>Section</b> %s &nbsp; <b>Row</b> %s &nbsp; <b>Seat</b> %d</p>
                <p>Show this code at the gate:</p>
                <img src="cid:qr" alt="Ticket QR code" width="300" height="300">
                <p style="color:#888">Ticket %s</p>
                """.formatted(
                HtmlUtils.htmlEscape(ticket.matchName()),
                HtmlUtils.htmlEscape(ticket.venueName()),
                KICK_OFF.format(ticket.startsAt()),
                HtmlUtils.htmlEscape(ticket.section()),
                HtmlUtils.htmlEscape(ticket.rowLabel()),
                ticket.seatNumber(),
                ticket.ticketId());
    }
}
