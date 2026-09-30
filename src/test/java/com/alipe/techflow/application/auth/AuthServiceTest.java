package com.alipe.techflow.application.auth;

import com.alipe.techflow.domain.model.User;
import com.alipe.techflow.domain.repository.UserRepository;
import com.alipe.techflow.infrastructure.security.JwtService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AuthServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    private final JwtService jwtService = mock(JwtService.class);
    private final AuthService service = new AuthService(users, encoder, authenticationManager, jwtService);

    @Test
    void shouldRegisterUserWithHashedPassword() {
        var request = new RegisterRequest("Alice", "ALICE@EXAMPLE.COM", "Senha@123");
        var saved = new User("Alice", "alice@example.com", encoder.encode(request.password()));
        var expected = new AuthResponse("token", "Bearer", 3600);

        given(users.existsByEmailIgnoreCase("alice@example.com")).willReturn(false);
        given(users.save(any(User.class))).willReturn(saved);
        given(jwtService.issue(saved)).willReturn(expected);

        var result = service.register(request);

        assertThat(result).isEqualTo(expected);
        var captor = ArgumentCaptor.forClass(User.class);
        verify(users).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("alice@example.com");
        assertThat(captor.getValue().getPasswordHash()).isNotEqualTo("Senha@123");
        assertThat(encoder.matches("Senha@123", captor.getValue().getPasswordHash())).isTrue();
    }

    @Test
    void shouldAuthenticateUserBeforeIssuingToken() {
        var user = new User("Alice", "alice@example.com", "hash");
        var response = new AuthResponse("token", "Bearer", 3600);
        given(users.findByEmailIgnoreCase("alice@example.com")).willReturn(Optional.of(user));
        given(jwtService.issue(user)).willReturn(response);

        var result = service.login(new LoginRequest("alice@example.com", "Senha@123"));

        assertThat(result).isEqualTo(response);
        verify(authenticationManager).authenticate(any());
        verify(jwtService).issue(user);
    }
}
