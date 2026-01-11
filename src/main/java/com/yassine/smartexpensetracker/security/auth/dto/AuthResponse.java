package com.yassine.smartexpensetracker.security.auth.dto;

public record AuthResponse(
        String accessToken,
        long expiresIn
) {}

