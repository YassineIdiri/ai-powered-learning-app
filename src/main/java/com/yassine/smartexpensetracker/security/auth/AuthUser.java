package com.yassine.smartexpensetracker.security.auth;

import java.util.UUID;

public record AuthUser(UUID id, String email) {}
