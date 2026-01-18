package com.yassine.expensetracker.security.auth.dto;

public record AuthResponse(
        String accessToken,
        long expiresIn
) {}

