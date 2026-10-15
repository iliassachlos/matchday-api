package com.matchday.order;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.matchday.order.service.TicketClientService;
import com.matchday.order.service.UserClientService;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTestBase {

    @MockitoBean
    protected TicketClientService ticketClientService;

    @MockitoBean
    protected UserClientService userClientService;
}
