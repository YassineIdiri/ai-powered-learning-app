package com.yassine.smartexpensetracker.security.auth;
import com.yassine.smartexpensetracker.security.auth.dto.AuthResponse;
import com.yassine.smartexpensetracker.security.auth.dto.ChangePasswordRequest;
import com.yassine.smartexpensetracker.security.auth.dto.LoginRequest;
import com.yassine.smartexpensetracker.security.auth.dto.RegisterRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        AuthService.AuthResult result = authService.register(req);
        return withCookie(result);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        AuthService.AuthResult result = authService.login(req); // req contient rememberMe
        return withCookie(result);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = "refresh_token", required = false) String refreshToken
    ) {
        AuthService.AuthResult result = authService.refresh(refreshToken);
        return withCookie(result);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "refresh_token", required = false) String refreshToken
    ) {
        authService.logout(refreshToken);

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, authService.clearRefreshCookie().toString())
                .build();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(@AuthenticationPrincipal AuthUser user) {
        authService.logoutEverywhere(user.id());

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, authService.clearRefreshCookie().toString())
                .build();
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal AuthUser user,
            @Valid @RequestBody ChangePasswordRequest req
    ) {
        authService.changePassword(user.id(), req.currentPassword(), req.newPassword());

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, authService.clearRefreshCookie().toString())
                .build();
    }

    private ResponseEntity<AuthResponse> withCookie(AuthService.AuthResult result) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok();

        if (result.refreshCookie() != null) {
            builder.header(HttpHeaders.SET_COOKIE, result.refreshCookie().toString());
        }

        return builder.body(result.response());
    }
}

