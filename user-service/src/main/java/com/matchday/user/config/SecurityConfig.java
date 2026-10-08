package com.matchday.user.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(RsaKeyProperties.class)
public class SecurityConfig {

  public static final String JWT_KEY_ID = "matchday-jwt-key";

  @Bean
  PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }

  @Bean
  JwtEncoder jwtEncoder(RsaKeyProperties keys) {
    return NimbusJwtEncoder.withKeyPair(keys.publicKey(), keys.privateKey())
        .jwkPostProcessor(jwk -> jwk.keyID(JWT_KEY_ID))
        .build();
  }

  @Bean
  JwtDecoder jwtDecoder(RsaKeyProperties keys) {
    return NimbusJwtDecoder.withPublicKey(keys.publicKey()).build();
  }

  @Bean
  JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();

    authorities.setAuthoritiesClaimName("roles");
    authorities.setAuthorityPrefix("");

    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

    converter.setJwtGrantedAuthoritiesConverter(authorities);

    return converter;
  }

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http.csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth -> auth.requestMatchers("/api/v1/auth/register", "/api/v1/auth/login")
                .permitAll()
                .requestMatchers("/error")
                .permitAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
                .permitAll()
                .requestMatchers("/actuator/health")
                .permitAll()
                .requestMatchers("/api/v1/admin/**")
                .hasRole("ADMIN")
                .anyRequest()
                .authenticated())
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
        .build();
  }
}
