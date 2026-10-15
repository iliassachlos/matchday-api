package com.matchday.order.service;

import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.matchday.order.dto.SaleOutcome;
import com.matchday.order.dto.SeatInfo;
import com.matchday.order.dto.SellRequest;
import com.matchday.order.exception.SeatNotFoundException;
import com.matchday.order.exception.TicketServiceUnavailableException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TicketClientService {
    private final RestClient ticketRestClient;

    /**
     * Call ticket-service to get information about a seat
     * 
     * @param seatId - Seat ID to get information about
     * @param bearerToken - Bearer token to authenticate with ticket-service
     * @return SeatInfo - Information about the seat
     */
    public SeatInfo getSeat(UUID seatId, String bearerToken) {
        try {
            return ticketRestClient.get()
                    .uri("/api/v1/seats/{id}", seatId)
                    .headers(headers -> headers.setBearerAuth(bearerToken))
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (request, response) -> {
                        throw new SeatNotFoundException();
                    })
                    .body(SeatInfo.class);
        } catch (RestClientException e) {
            throw new TicketServiceUnavailableException("Could not read seat " + seatId, e);
        }
    }

    /**
     *  Call ticket-service to sell a seat
     * 
     * @param seatId - Seat ID to sell
     * @param orderId - Order ID to associate with the sale
     * @param bearerToken - Bearer token to authenticate with ticket-service
     * @return Outcome of the sale
     */
    public SaleOutcome sell(UUID seatId, UUID orderId, String bearerToken) {
        try {
            return ticketRestClient.post()
                    .uri("/api/v1/seats/{id}/sell", seatId)
                    .headers(headers -> headers.setBearerAuth(bearerToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new SellRequest(orderId))
                    .exchange((request, response) -> {
                        if (response.getStatusCode().is2xxSuccessful()) {
                            return SaleOutcome.SOLD;
                        }
                        if (response.getStatusCode().value() == 409) {
                            return SaleOutcome.REJECTED;
                        }
                        throw new TicketServiceUnavailableException(
                                "ticket-service answered " + response.getStatusCode() + " to sell " + seatId);
                    });
        } catch (RestClientException e) {
            throw new TicketServiceUnavailableException("Could not reach ticket-service to sell " + seatId, e);
        }
    }
}
