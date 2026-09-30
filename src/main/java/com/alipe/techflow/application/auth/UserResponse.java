package com.alipe.techflow.application.auth;

import com.alipe.techflow.domain.model.User;
import com.alipe.techflow.domain.model.Role;
import java.time.OffsetDateTime;

public record UserResponse(Long id, String name, String email, Role role, OffsetDateTime createdAt) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getCreatedAt());
    }
}
