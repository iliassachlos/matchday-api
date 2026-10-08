package com.matchday.user.contoller;

import com.matchday.user.dto.LoginRequest;
import com.matchday.user.dto.RegisterRequest;
import com.matchday.user.dto.TokenResponse;
import com.matchday.user.dto.UserResponse;
import com.matchday.user.service.UserService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @PostMapping("/register")
  public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
    UserResponse user = userService.register(request);

    return ResponseEntity.created(URI.create("/api/v1/users/" + user.id())).body(user);
  }

  @PostMapping("/login")
  public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
    TokenResponse tokenResponse = userService.login(request);

    return ResponseEntity.ok(tokenResponse);
  }

  @GetMapping("/me")
  public UserResponse getUserById(@AuthenticationPrincipal Jwt jwt) {
    UUID userId = UUID.fromString(jwt.getSubject());

    return userService.getById(userId);
  }

}
