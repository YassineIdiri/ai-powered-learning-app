package com.yassine.expensetracker.security.auth;

import com.yassine.expensetracker.security.auth.dto.AuthResponse;
import com.yassine.expensetracker.security.auth.dto.LoginRequest;
import com.yassine.expensetracker.security.auth.dto.RegisterRequest;
import com.yassine.expensetracker.security.jwt.JwtService;
import com.yassine.expensetracker.security.refresh.RefreshTokenService;
import com.yassine.expensetracker.user.User;
import com.yassine.expensetracker.user.UserRepository;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {

    public static final long ACCESS_TTL_SECONDS = 15 * 60; // 15 minutes

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final AuthCookieProps cookieProps;

    public AuthService(
            UserRepository userRepository,
            JwtService jwtService,
            AuthenticationManager authenticationManager,
            PasswordEncoder passwordEncoder,
            RefreshTokenService refreshTokenService,
            AuthCookieProps cookieProps
    ) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
        this.cookieProps = cookieProps;
    }

    @Transactional
    public AuthResult register(RegisterRequest req) {
        String email = normalizeEmail(req.email());

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already used");
        }

        User u = new User();
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(req.password()));
        userRepository.save(u);

        return buildAuthResult(u.getId(), u.getEmail(), false);
    }

    @Transactional(readOnly = true)
    public AuthResult login(LoginRequest req) {
        String email = normalizeEmail(req.email());

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, req.password())
        );

        User u = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        return buildAuthResult(u.getId(), u.getEmail(), req.rememberMe());
    }

    @Transactional
    public AuthResult refresh(String rawRefreshToken) {
        RefreshTokenService.RotationResult rotated =
                refreshTokenService.verifyAndRotate(rawRefreshToken);

        User u = userRepository.findById(rotated.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String accessToken = jwtService.generateToken(
                u.getId(),
                u.getEmail(),
                ACCESS_TTL_SECONDS
        );

        ResponseCookie refreshCookie = buildRefreshCookie(
                rotated.newRawRefreshToken(),
                rotated.expiresAt()
        );

        return new AuthResult(
                new AuthResponse(accessToken, ACCESS_TTL_SECONDS),
                refreshCookie
        );
    }

    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid current password");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        refreshTokenService.revokeAllForUser(userId);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

    @Transactional
    public void logoutEverywhere(UUID userId) {
        refreshTokenService.revokeAllForUser(userId);
    }

    public ResponseCookie clearRefreshCookie() {
        return ResponseCookie.from(cookieProps.refreshCookieName, "")
                .httpOnly(true)
                .secure(cookieProps.cookieSecure)
                .sameSite(cookieProps.sameSite)
                .path(cookieProps.refreshCookiePath)
                .maxAge(0)
                .build();
    }

    // ----------------- internals -----------------

    private AuthResult buildAuthResult(UUID userId, String email, boolean rememberMe) {
        String accessToken = jwtService.generateToken(userId, email, ACCESS_TTL_SECONDS);

        RefreshTokenService.IssueResult issued =
                refreshTokenService.issue(userId, rememberMe);

        ResponseCookie refreshCookie = buildRefreshCookie(
                issued.rawRefreshToken(),
                issued.expiresAt()
        );

        return new AuthResult(
                new AuthResponse(accessToken, ACCESS_TTL_SECONDS),
                refreshCookie
        );
    }

    private ResponseCookie buildRefreshCookie(String rawRefreshToken, Instant expiresAt) {
        long maxAgeSeconds = Math.max(0, Duration.between(Instant.now(), expiresAt).getSeconds());

        return ResponseCookie.from(cookieProps.refreshCookieName, rawRefreshToken)
                .httpOnly(true)
                .secure(cookieProps.cookieSecure)
                .sameSite(cookieProps.sameSite)
                .path(cookieProps.refreshCookiePath)
                .maxAge(maxAgeSeconds)
                .build();
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    public record AuthResult(AuthResponse response, ResponseCookie refreshCookie) {}
}
