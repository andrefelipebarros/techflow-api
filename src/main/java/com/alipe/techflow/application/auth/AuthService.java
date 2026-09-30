package com.alipe.techflow.application.auth;

import com.alipe.techflow.domain.model.User;
import com.alipe.techflow.domain.repository.UserRepository;
import com.alipe.techflow.infrastructure.security.JwtService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        var email = request.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("E-mail já cadastrado");
        }
        try {
            var user = users.save(new User(request.name().trim(), email, passwordEncoder.encode(request.password())));
            return jwtService.issue(user);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("E-mail já cadastrado");
        }
    }

    public AuthResponse login(LoginRequest request) {
        var email = request.email().trim().toLowerCase();
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));
        return jwtService.issue(users.findByEmailIgnoreCase(email).orElseThrow());
    }
}
