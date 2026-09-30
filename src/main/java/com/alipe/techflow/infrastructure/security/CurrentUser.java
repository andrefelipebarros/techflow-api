package com.alipe.techflow.infrastructure.security;

import com.alipe.techflow.domain.model.User;
import com.alipe.techflow.domain.repository.UserRepository;
import java.util.NoSuchElementException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {
    private final UserRepository users;

    public CurrentUser(UserRepository users) {
        this.users = users;
    }

    public User get() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Usuário não autenticado");
        }
        return users.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new NoSuchElementException("Usuário autenticado não encontrado"));
    }

    public Long id() {
        return get().getId();
    }
}
