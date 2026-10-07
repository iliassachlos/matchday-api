package com.matchday.user.mapper;

import com.matchday.user.dto.UserResponse;
import com.matchday.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
  public UserResponse toResponse(User user) {
    return new UserResponse(
        user.getId(), user.getEmail(), user.getDisplayName(), user.getRoles(), user.getCreatedAt());
  }
}
