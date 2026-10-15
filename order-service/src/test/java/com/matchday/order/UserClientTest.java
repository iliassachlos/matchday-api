package com.matchday.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.matchday.order.dto.UserInfo;
import com.matchday.order.exception.UserServiceUnavailableException;
import com.matchday.order.service.UserClient;

class UserClientTest {

    private static final String BASE = "http://user-service";
    private static final String TOKEN = "maria-token";
    private static final UUID MARIA = UUID.randomUUID();

    private MockRestServiceServer server;
    private UserClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new UserClient(builder.build());
    }

    @Test
    @DisplayName("asks user-service who the buyer is, with the buyer's own token")
    void readsCurrentUser() {
        server.expect(requestTo(BASE + "/api/v1/auth/me"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andRespond(withSuccess(
                        "{\"id\":\"" + MARIA + "\",\"email\":\"maria@test.gr\",\"displayName\":\"Maria\",\"roles\":[\"ROLE_USER\"]}",
                        MediaType.APPLICATION_JSON));

        UserInfo user = client.getCurrentUser(TOKEN);

        assertThat(user.email()).isEqualTo("maria@test.gr");
        assertThat(user.id()).isEqualTo(MARIA);
        server.verify();
    }

    @Test
    @DisplayName("user-service failing is reported as unavailable, not as a crash")
    void userServiceDown() {
        server.expect(requestTo(BASE + "/api/v1/auth/me"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.getCurrentUser(TOKEN))
                .isInstanceOf(UserServiceUnavailableException.class);
    }
}
