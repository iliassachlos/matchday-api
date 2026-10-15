package com.matchday.user.service;

import com.matchday.user.dto.LoginRequest;
import com.matchday.user.dto.RegisterRequest;
import com.matchday.user.dto.TokenResponse;
import com.matchday.user.dto.UserResponse;
import com.matchday.user.entity.User;
import com.matchday.user.exception.EmailAlreadyUsedException;
import com.matchday.user.exception.InvalidCredentialsException;
import com.matchday.user.exception.UserNotFoundException;

import com.matchday.user.repository.UserRepository;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

  private final TokenService tokenService;

  private final UserRepository userRepository;

  private final PasswordEncoder passwordEncoder;

  /**
   * Register a new user
   * 
   * @param request - The RegisterRequest containing the user's email, password,
   *                and display name
   * @return A UserResponse containing the registered user's information
   */
  public UserResponse register(RegisterRequest request) {
    String email = request.email().trim().toLowerCase(Locale.ROOT);

    if (userRepository.existsByEmail(email)) {
      throw new EmailAlreadyUsedException();
    }

    String passwordHash = passwordEncoder.encode(request.password());
    User user = User.register(email, passwordHash, request.displayName().trim());

    return UserResponse.from(userRepository.save(user));
  }

  /**
   * Login a user and return a JWT token
   * 
   * @param request - The LoginRequest containing the user's email and password
   * @return A TokenResponse containing the JWT token and its expiration time
   * @throws InvalidCredentialsException if the email or password is invalid
   */
  public TokenResponse login(LoginRequest request) {
    String email = request.email().trim().toLowerCase(Locale.ROOT);
    User user = userRepository.findByEmail(email).orElseThrow(InvalidCredentialsException::new);

    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new InvalidCredentialsException();
    }

    return new TokenResponse(tokenService.mintAccessToken(user), "Bearer", tokenService.accessTokenTtlSeconds());
  }

  /**
   * Get a user by their ID
   * 
   * @param id - The UUID of the user to retrieve
   * @return A UserResponse containing the user's information
   * @throws UserNotFoundException if the user with the given ID does not exist
   */
  public UserResponse getById(UUID id) {
    User user = userRepository.findById(id).orElseThrow(UserNotFoundException::new);

    return UserResponse.from(user);
  }

  /**
   *  Get all users in the system
   * 
   * @return - A list of UserResponse objects containing the information of all users
   */
  public List<UserResponse> getAll() {
    List<User> users = userRepository.findAll();

    return users.stream().map(UserResponse::from).toList();
  }
}
