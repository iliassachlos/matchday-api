package com.matchday.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.matchday.order.dto.SaleOutcome;
import com.matchday.order.dto.SeatInfo;
import com.matchday.order.exception.SeatNotFoundException;
import com.matchday.order.exception.TicketServiceUnavailableException;
import com.matchday.order.service.TicketClientService;

// Plain unit test: no Spring context, no database, no running ticket-service
class TicketClientTest {

    private static final String BASE = "http://ticket-service";
    private static final String TOKEN = "maria-token";
    private static final UUID SEAT = UUID.randomUUID();
    private static final UUID ORDER = UUID.randomUUID();

    private MockRestServiceServer server;
    private TicketClientService client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new TicketClientService(builder.build());
    }

    @Test
    @DisplayName("reads the seat's price, forwarding the buyer's token")
    void readsSeat() {
        server.expect(requestTo(BASE + "/api/v1/seats/" + SEAT))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andRespond(withSuccess(
                        "{\"id\":\"" + SEAT + "\",\"section\":\"14\",\"priceCents\":4500,\"status\":\"RESERVED\"}",
                        MediaType.APPLICATION_JSON));

        SeatInfo seat = client.getSeat(SEAT, TOKEN);

        assertThat(seat.priceCents()).isEqualTo(4500);
        server.verify();
    }

    @Test
    @DisplayName("an unknown seat is a 404, not an outage")
    void unknownSeat() {
        server.expect(requestTo(BASE + "/api/v1/seats/" + SEAT))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.getSeat(SEAT, TOKEN)).isInstanceOf(SeatNotFoundException.class);
    }

    @Test
    @DisplayName("200 from sell means the seat is ours")
    void sellAccepted() {
        server.expect(requestTo(BASE + "/api/v1/seats/" + SEAT + "/sell"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andExpect(jsonPath("$.orderId").value(ORDER.toString()))
                .andRespond(withSuccess());

        assertThat(client.sell(SEAT, ORDER, TOKEN)).isEqualTo(SaleOutcome.SOLD);
        server.verify();
    }

    @Test
    @DisplayName("409 from sell is an answer (rejected), not an error")
    void sellRejected() {
        server.expect(requestTo(BASE + "/api/v1/seats/" + SEAT + "/sell"))
                .andRespond(withStatus(HttpStatus.CONFLICT));

        assertThat(client.sell(SEAT, ORDER, TOKEN)).isEqualTo(SaleOutcome.REJECTED);
    }

    @Test
    @DisplayName("a 500 from ticket-service means we do not know what happened")
    void sellUnknown() {
        server.expect(requestTo(BASE + "/api/v1/seats/" + SEAT + "/sell"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.sell(SEAT, ORDER, TOKEN))
                .isInstanceOf(TicketServiceUnavailableException.class);
    }
}
