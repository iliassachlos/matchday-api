package com.matchday.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.matchday.order.dto.SaleOutcome;
import com.matchday.order.dto.SeatInfo;
import com.matchday.order.entity.Order;
import com.matchday.order.entity.OrderStatus;
import com.matchday.order.exception.TicketServiceUnavailableException;
import com.matchday.order.repository.OrderRepository;
import com.matchday.order.service.PaymentService;

class OrderFlowTest extends IntegrationTestBase {

    private static final UUID MARIA = UUID.randomUUID();
    private static final UUID GIORGOS = UUID.randomUUID();
    private static final UUID SEAT = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAllInBatch();
        when(ticketClient.getSeat(eq(SEAT), any())).thenReturn(new SeatInfo(SEAT, 4500, "RESERVED"));
    }

    private RequestPostProcessor as(UUID userId) {
        return jwt().jwt(builder -> builder.subject(userId.toString()));
    }

    private ResultActions placeOrderAs(UUID userId, String paymentMethod) throws Exception {
        return mockMvc.perform(post("/api/v1/orders")
                .with(as(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"seatId\":\"" + SEAT + "\",\"paymentMethod\":\"" + paymentMethod + "\"}"));
    }

    private Order onlyOrder() {
        assertThat(orderRepository.count()).isEqualTo(1);
        return orderRepository.findAll().getFirst();
    }

    @Test
    @DisplayName("charged and sold: the order is PAID at the seat's price")
    void paidWhenChargedAndSold() throws Exception {
        when(ticketClient.sell(eq(SEAT), any(), any())).thenReturn(SaleOutcome.SOLD);

        placeOrderAs(MARIA, PaymentService.CARD_OK)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.amountCents").value(4500));

        Order order = onlyOrder();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(order.getPaymentReference()).startsWith("fake_ch_");
    }

    @Test
    @DisplayName("a declined card fails the order and never touches the seat")
    void failedWhenCardDeclined() throws Exception {
        placeOrderAs(MARIA, PaymentService.CARD_DECLINED)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FAILED"));

        verify(ticketClient, never()).sell(any(), any(), any());
    }

    @Test
    @DisplayName("charged but ticket-service says no (hold expired): the money goes back")
    void refundedWhenSaleRejected() throws Exception {
        when(ticketClient.sell(eq(SEAT), any(), any())).thenReturn(SaleOutcome.REJECTED);

        placeOrderAs(MARIA, PaymentService.CARD_OK)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REFUNDED"));
    }

    @Test
    @DisplayName("a timeout is retried with the same order, and the retry confirms the sale")
    void retriedAfterTimeoutThenPaid() throws Exception {
        when(ticketClient.sell(eq(SEAT), any(), any()))
                .thenThrow(new TicketServiceUnavailableException("read timed out"))
                .thenReturn(SaleOutcome.SOLD);

        placeOrderAs(MARIA, PaymentService.CARD_OK)
                .andExpect(jsonPath("$.status").value("PAID"));

        UUID orderId = onlyOrder().getId();
        verify(ticketClient, times(2)).sell(SEAT, orderId, "token");
    }

    @Test
    @DisplayName("ticket-service stays down: give up after three attempts and refund")
    void refundedWhenTicketServiceStaysDown() throws Exception {
        when(ticketClient.sell(eq(SEAT), any(), any()))
                .thenThrow(new TicketServiceUnavailableException("connection refused"));

        placeOrderAs(MARIA, PaymentService.CARD_OK)
                .andExpect(jsonPath("$.status").value("REFUNDED"));

        verify(ticketClient, times(3)).sell(eq(SEAT), any(), any());
    }

    @Test
    @DisplayName("nobody is charged for a seat they have not reserved")
    void notChargedWhenSeatNotReserved() throws Exception {
        when(ticketClient.getSeat(eq(SEAT), any())).thenReturn(new SeatInfo(SEAT, 4500, "AVAILABLE"));

        placeOrderAs(MARIA, PaymentService.CARD_OK).andExpect(status().isConflict());

        assertThat(orderRepository.count()).isZero();
    }

    @Test
    @DisplayName("Giorgos cannot see Maria's orders")
    void ordersAreScopedToTheCaller() throws Exception {
        when(ticketClient.sell(eq(SEAT), any(), any())).thenReturn(SaleOutcome.SOLD);
        placeOrderAs(MARIA, PaymentService.CARD_OK);

        mockMvc.perform(get("/api/v1/orders/me").with(as(MARIA)))
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/v1/orders/me").with(as(GIORGOS)))
                .andExpect(jsonPath("$.length()").value(0));
    }
}
