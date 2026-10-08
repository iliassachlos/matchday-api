package com.matchday.user.service;

import com.matchday.user.dto.LoginRequest;
import com.matchday.user.dto.RegisterRequest;
import com.matchday.user.dto.TokenResponse;
import com.matchday.user.dto.UserResponse;
import com.matchday.user.entity.User;
import com.matchday.user.exception.EmailAlreadyUsedException;
import com.matchday.user.exception.InvalidCredentialsException;
import com.matchday.user.mapper.UserMapper;
import com.matchday.user.repository.UserRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

  private final TokenService tokenService;

  private final UserRepository userRepository;

  private final PasswordEncoder passwordEncoder;

  private final UserMapper userMapper;

  // Register a new user
  public UserResponse register(RegisterRequest request) {
    String email = request.email().trim().toLowerCase(Locale.ROOT);

    if (userRepository.existsByEmail(email)) {
      throw new EmailAlreadyUsedException();
    }

    String passwordHash = passwordEncoder.encode(request.password());
    User user = User.register(email, passwordHash, request.displayName().trim());

    return userMapper.toResponse(userRepository.save(user));
  }

  // Login an existing user
  public TokenResponse login(LoginRequest request) {
    String email = request.email().trim().toLowerCase(Locale.ROOT);
    User user = userRepository.findByEmail(email).orElseThrow(InvalidCredentialsException::new);

    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new InvalidCredentialsException();
    }

    return new TokenResponse(tokenService.mintAccessToken(user), "Bearer", tokenService.accessTokenTtlSeconds());
  }
}
 