package com.alipe.techflow.interfaces.rest;

import com.alipe.techflow.application.auth.UserResponse;
import com.alipe.techflow.domain.repository.UserRepository;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
    private final UserRepository users;

    public AdminController(UserRepository users) {
        this.users = users;
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<UserResponse> users(Pageable pageable) {
        return users.findAll(pageable).map(UserResponse::from);
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Long> summary() {
        return Map.of("users", users.count());
    }
}
