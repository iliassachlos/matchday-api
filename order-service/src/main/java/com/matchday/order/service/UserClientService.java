package com.matchday.order.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.matchday.order.dto.UserInfo;
import com.matchday.order.exception.UserServiceUnavailableException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserClientService {

    private final RestClient userRestClient;

    public UserInfo getCurrentUser(String token) {
        try {
            return userRestClient.get()
                    .uri("/api/v1/auth/me")
                    .headers(headers -> headers.setBearerAuth(token))
                    .retrieve()
                    .body(UserInfo.class);
        } catch (Exception e) {
            throw new UserServiceUnavailableException("Could not read the buyer's details from user-service", e);
        }
    }
}
