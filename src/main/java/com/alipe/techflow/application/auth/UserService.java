package com.alipe.techflow.application.auth;

import com.alipe.techflow.infrastructure.security.CurrentUser;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final CurrentUser currentUser;

    public UserService(CurrentUser currentUser) {
        this.currentUser = currentUser;
    }

    public UserResponse me() {
        return UserResponse.from(currentUser.get());
    }
}
