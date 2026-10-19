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

  /**
   * Create a PasswordEncoder bean that uses the default encoding algorithm
   * 
   * @return A PasswordEncoder that uses the default encoding algorithm
   */
  @Bean
  PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }

  /**
   * Create a JwtEncoder bean that uses the public and private keys from the
   * application
   * 
   * @param keys - The RsaKeyProperties bean that contains the public and private
   *             keys
   * @return A JwtEncoder that uses the public and private keys to encode JWTs
   */
  @Bean
  JwtEncoder jwtEncoder(RsaKeyProperties keys) {
    return NimbusJwtEncoder.withKeyPair(keys.publicKey(), keys.privateKey())
        .jwkPostProcessor(jwk -> jwk.keyID(JWT_KEY_ID))
        .build();
  }

  /**
   * Create a JwtDecoder bean that uses the public key from the application
   * properties to decode JWTs.
   * 
   * @param keys - The JwtKeyProperties bean that contains the public key
   * @return A JwtDecoder that uses the public key to decode JWTs
   */
  @Bean
  JwtDecoder jwtDecoder(RsaKeyProperties keys) {
    return NimbusJwtDecoder.withPublicKey(keys.publicKey()).build();
  }

  /**
   * Create a JwtAuthenticationConverter bean that converts JWTs into Spring
   * Security
   * 
   * @return A JwtAuthenticationConverter that converts JWTs into Spring Security
   */
  @Bean
  JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();

    authorities.setAuthoritiesClaimName("roles");
    authorities.setAuthorityPrefix("");

    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

    converter.setJwtGrantedAuthoritiesConverter(authorities);

    return converter;
  }

  /**
   * Configure the security filter chain for the application
   * 
   * @param http - The HttpSecurity object to configure
   * @return A SecurityFilterChain that defines the security configuration for the
   *         application
   * @throws Exception
   */
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
                .requestMatchers("/actuator/health", "/actuator/prometheus")
                .permitAll()
                .requestMatchers("/api/v1/admin/**")
                .hasRole("ADMIN")
                .anyRequest()
                .authenticated())
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
        .build();
  }
}
