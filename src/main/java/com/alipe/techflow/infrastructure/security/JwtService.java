package com.alipe.techflow.infrastructure.security;

import com.alipe.techflow.application.auth.AuthResponse;
import com.alipe.techflow.domain.model.User;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.JwsHeader;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final JwtEncoder encoder;
    private final String issuer;
    private final long expirationMinutes;

    public JwtService(JwtEncoder encoder,
                      @Value("${app.jwt.issuer}") String issuer,
                      @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
        this.encoder = encoder;
        this.issuer = issuer;
        this.expirationMinutes = expirationMinutes;
    }

    public AuthResponse issue(User user) {
        var now = Instant.now();
        var expiresAt = now.plus(expirationMinutes, ChronoUnit.MINUTES);
        var claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(user.getEmail())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim("roles", java.util.List.of(user.getRole().name()))
                .build();

        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        var token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AuthResponse(token, "Bearer", expirationMinutes * 60);
    }
}
