package com.matchday.order.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class HttpClientsConfig {

    /**
     * Creates a RestClient bean for the ticket service with specified timeouts.
     * 
     * @param baseUrl - the base URL of the ticket service
     * @return RestClient configured with timeouts
     */
    @Bean
    RestClient ticketRestClient(@Value("${matchday.ticket-service.url}") String baseUrl) {
        return withTimeouts(baseUrl);
    }

    /**
     * Creates a RestClient bean for the user service with specified timeouts.
     * 
     * @param baseUrl - the base URL of the user service
     * @return RestClient configured with timeouts
     */
    @Bean
    RestClient userRestClient(@Value("${matchday.user-service.url}") String baseUrl) {
        return withTimeouts(baseUrl);
    }

    /**
     * Creates a RestClient with specified timeouts for the given base URL.
     * 
     * @param baseUrl - the base URL for the RestClient
     * @return RestClient configured with timeouts
     */
    private static RestClient withTimeouts(String baseUrl) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(3));

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
