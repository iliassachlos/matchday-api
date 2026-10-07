package com.matchday.user.service;

import com.matchday.user.dto.RegisterRequest;
import com.matchday.user.dto.UserResponse;
import com.matchday.user.entity.User;
import com.matchday.user.exception.EmailAlreadyUsedException;
import com.matchday.user.mapper.UserMapper;
import com.matchday.user.repository.UserRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final UserMapper userMapper;

  // Register a new user
  @Transactional
  public UserResponse register(RegisterRequest request) {
    String email = request.email().trim().toLowerCase(Locale.ROOT);

    if (userRepository.existsByEmail(email)) {
      throw new EmailAlreadyUsedException();
    }

    String passwordHash = passwordEncoder.encode(request.password());
    User user = User.register(email, passwordHash, request.displayName().trim());

    return userMapper.toResponse(userRepository.save(user));
  }
}
