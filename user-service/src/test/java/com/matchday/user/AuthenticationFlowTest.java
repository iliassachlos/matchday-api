package com.matchday.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.matchday.user.entity.Role;
import com.matchday.user.entity.User;
import com.matchday.user.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

class AuthenticationFlowTest extends IntegrationTestBase {

    private static final String EMAIL = "maria@matchday.test";
    private static final String PASSWORD = "correct-horse-battery";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    private String registerPayload(String email, String password, String displayName) {
        return """
                {"email": "%s", "password": "%s", "displayName": "%s"}
                """.formatted(email, password, displayName);
    }

    private String loginPayload(String email, String password) {
        return """
                {"email": "%s", "password": "%s"}
                """.formatted(email, password);
    }

    private String registerAndLogin() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload(EMAIL, PASSWORD, "Maria")))
                .andExpect(status().isCreated());

        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload(EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return body.replaceAll(".*\"accessToken\"\\s*:\\s*\"([^\"]+)\".*", "$1");
    }

    @Test
    @DisplayName("register creates the user, assigns ROLE_USER and never echoes the password")
    void registerCreatesUser() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload(EMAIL, PASSWORD, "Maria")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.displayName").value("Maria"))
                .andExpect(jsonPath("$.roles[0]").value(Role.ROLE_USER.name()))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("the password is stored as a BCrypt hash, never in plain text")
    void passwordIsHashed() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload(EMAIL, PASSWORD, "Maria")))
                .andExpect(status().isCreated());

        User stored = userRepository.findByEmail(EMAIL).orElseThrow();

        assertThat(stored.getPasswordHash()).isNotEqualTo(PASSWORD);
        assertThat(stored.getPasswordHash()).contains("bcrypt");
        assertThat(passwordEncoder.matches(PASSWORD, stored.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("registering the same email twice is a 409, not a second account")
    void duplicateEmailIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload(EMAIL, PASSWORD, "Maria")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload(EMAIL, PASSWORD, "Someone Else")))
                .andExpect(status().isConflict());

        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("an invalid payload is a 400 with per-field messages, not a 500")
    void invalidPayloadIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload("not-an-email", "short", "Maria")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());

        assertThat(userRepository.count()).isZero();
    }

    @Test
    @DisplayName("login returns a bearer token for the right password")
    void loginReturnsToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload(EMAIL, PASSWORD, "Maria")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload(EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").isNumber());
    }

    @Test
    @DisplayName("login with the wrong password is 401 and says nothing about which part was wrong")
    void loginRejectsWrongPassword() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload(EMAIL, PASSWORD, "Maria")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload(EMAIL, "wrong-password")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("login for an unknown email is also 401, so accounts cannot be enumerated")
    void loginRejectsUnknownEmail() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload("ghost@matchday.test", PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("/me without a token is 401")
    void meRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("/me with a garbage token is 401, not 500")
    void meRejectsGarbageToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("a token minted at login identifies its owner on /me")
    void meReturnsTheAuthenticatedUser() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.displayName").value("Maria"));
    }
}
