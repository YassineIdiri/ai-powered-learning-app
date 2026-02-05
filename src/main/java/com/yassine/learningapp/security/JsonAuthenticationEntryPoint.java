package com.yassine.learningapp.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

@Component
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final String ATTR_CODE = "auth_error_code";
    private static final String ATTR_MESSAGE = "auth_error_message";

    private final ObjectMapper objectMapper;

    public JsonAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public record ApiError(
            Instant timestamp,
            int status,
            String error,
            String code,
            String message,
            String path,
            String traceId
    ) { }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException ex) throws IOException {

        // 1) Priorité: ce que le filter a défini (utile pour TOKEN_EXPIRED, INVALID_SIGNATURE, etc.)
        String code = asString(request.getAttribute(ATTR_CODE));
        String message = asString(request.getAttribute(ATTR_MESSAGE));

        // 2) Sinon: mapper depuis l'exception Spring Security
        if (isBlank(code)) {
            code = mapCode(ex);
        }
        if (isBlank(message)) {
            message = mapMessage(ex, code);
        }

        // 3) TraceId (si tu as un MDC, sinon null)
        String traceId = MDC.get("traceId"); // optionnel: depends de ta config logs
        if (isBlank(traceId)) {
            // certains systèmes utilisent "X-B3-TraceId" ou "traceparent" etc.
            traceId = request.getHeader("X-Request-Id");
        }

        ApiError body = new ApiError(
                Instant.now(),
                HttpServletResponse.SC_UNAUTHORIZED,
                "UNAUTHORIZED",
                code,
                message,
                request.getRequestURI(),
                traceId
        );

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    private String mapCode(AuthenticationException ex) {
        if (ex instanceof DisabledException) return "ACCOUNT_DISABLED";
        if (ex instanceof LockedException) return "ACCOUNT_LOCKED";
        if (ex instanceof UsernameNotFoundException) return "USER_NOT_FOUND";
        if (ex instanceof BadCredentialsException) {
            // Souvent on met le "code" dans le message : ex: "TOKEN_EXPIRED", "INVALID_TOKEN"
            String msg = ex.getMessage();
            return isBlank(msg) ? "INVALID_CREDENTIALS" : msg;
        }
        return "UNAUTHORIZED";
    }

    private String mapMessage(AuthenticationException ex, String code) {
        // Messages "safe" côté client : pas de détails de parsing JWT
        return switch (code) {
            case "TOKEN_EXPIRED" -> "Token expiré";
            case "INVALID_SIGNATURE", "MALFORMED_TOKEN", "UNSUPPORTED_TOKEN", "INVALID_TOKEN", "INVALID_CREDENTIALS" ->
                    "Token invalide";
            case "ACCOUNT_DISABLED" -> "Compte désactivé";
            case "ACCOUNT_LOCKED" -> "Compte verrouillé";
            case "USER_NOT_FOUND" -> "Utilisateur non trouvé";
            default -> "Non authentifié";
        };
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
