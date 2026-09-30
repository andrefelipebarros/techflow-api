package com.alipe.techflow.application.auth;

public record AuthResponse(String accessToken, String tokenType, long expiresInSeconds) {}
