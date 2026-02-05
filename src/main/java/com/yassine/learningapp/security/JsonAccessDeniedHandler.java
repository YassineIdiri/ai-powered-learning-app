package com.yassine.learningapp.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

@Component
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public JsonAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // ════════════════════════════════════════════════════════
    // Record pour la réponse d'erreur
    // ════════════════════════════════════════════════════════
    public record ApiError(
            Instant timestamp,
            int status,
            String error,
            String code,           // ← Code précis
            String message,        // ← Message détaillé
            String path,
            String traceId,
            Map<String, Object> details  // ← Infos supplémentaires
    ) {}

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException ex) throws IOException, ServletException {

        // Récupérer le traceId (pour le tracking)
        String traceId = MDC.get("traceId");
        if (traceId == null || traceId.isBlank()) {
            traceId = request.getHeader("X-Request-Id");
        }

        // ✅ AMÉLIORATION PRINCIPALE : Mapper l'erreur selon le contexte
        ErrorInfo errorInfo = mapErrorInfo(ex, request);

        ApiError body = new ApiError(
                Instant.now(),
                HttpServletResponse.SC_FORBIDDEN,
                "FORBIDDEN",
                errorInfo.code(),      // ← Code spécifique
                errorInfo.message(),   // ← Message spécifique
                request.getRequestURI(),
                traceId,
                errorInfo.details()    // ← Détails supplémentaires
        );

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    // ════════════════════════════════════════════════════════
    // LOGIQUE DE DÉTECTION : Analyser l'exception et l'URL
    // ════════════════════════════════════════════════════════
    private ErrorInfo mapErrorInfo(AccessDeniedException ex, HttpServletRequest request) {
        String exMessage = ex.getMessage();
        String path = request.getRequestURI();

        // ────────────────────────────────────────────────────
        // CAS 1 : Routes admin (détection par URL)
        // ────────────────────────────────────────────────────
        if (path.startsWith("/api/admin")) {
            return new ErrorInfo(
                    "ADMIN_ACCESS_REQUIRED",
                    "Accès administrateur requis",
                    Map.of(
                            "requiredRole", "ROLE_ADMIN",
                            "suggestion", "Cette fonctionnalité est réservée aux administrateurs"
                    )
            );
        }

        // ────────────────────────────────────────────────────
        // CAS 2 : Rôle insuffisant (détection dans le message)
        // ────────────────────────────────────────────────────
        if (exMessage != null && exMessage.contains("ROLE_")) {
            String requiredRole = extractRole(exMessage);
            return new ErrorInfo(
                    "INSUFFICIENT_ROLE",
                    "Rôle insuffisant pour cette action",
                    Map.of(
                            "requiredRole", requiredRole,
                            "suggestion", "Contactez un administrateur pour obtenir les permissions"
                    )
            );
        }

        // ────────────────────────────────────────────────────
        // CAS 3 : Propriétaire requis (détection mot-clé "owner")
        // ────────────────────────────────────────────────────
        if (exMessage != null && exMessage.toLowerCase().contains("owner")) {
            return new ErrorInfo(
                    "NOT_RESOURCE_OWNER",
                    "Vous n'êtes pas propriétaire de cette ressource",
                    Map.of(
                            "suggestion", "Vous ne pouvez modifier que vos propres ressources"
                    )
            );
        }

        // ────────────────────────────────────────────────────
        // CAS 4 : Premium requis (détection mot-clé "premium")
        // ────────────────────────────────────────────────────
        if (exMessage != null &&
                (exMessage.toLowerCase().contains("premium") ||
                        exMessage.toLowerCase().contains("subscription"))) {
            return new ErrorInfo(
                    "PREMIUM_REQUIRED",
                    "Abonnement premium requis",
                    Map.of(
                            "suggestion", "Passez à la version premium pour accéder à cette fonctionnalité",
                            "upgradeUrl", "/api/subscription/upgrade"
                    )
            );
        }

        // ────────────────────────────────────────────────────
        // CAS 5 : Email non vérifié
        // ────────────────────────────────────────────────────
        if (exMessage != null && exMessage.toLowerCase().contains("email not verified")) {
            return new ErrorInfo(
                    "EMAIL_NOT_VERIFIED",
                    "Email non vérifié",
                    Map.of(
                            "suggestion", "Veuillez vérifier votre email avant d'accéder à cette fonctionnalité",
                            "resendUrl", "/api/auth/resend-verification"
                    )
            );
        }

        // ────────────────────────────────────────────────────
        // CAS PAR DÉFAUT : Message générique
        // ────────────────────────────────────────────────────
        return new ErrorInfo(
                "FORBIDDEN",
                "Accès refusé",
                Map.of(
                        "suggestion", "Vous n'avez pas les permissions nécessaires"
                )
        );
    }

    // ════════════════════════════════════════════════════════
    // Méthode utilitaire : Extraire le rôle du message
    // ════════════════════════════════════════════════════════
    private String extractRole(String message) {
        if (message.contains("ROLE_ADMIN")) return "ROLE_ADMIN";
        if (message.contains("ROLE_MODERATOR")) return "ROLE_MODERATOR";
        if (message.contains("ROLE_PREMIUM")) return "ROLE_PREMIUM";
        return "ROLE_UNKNOWN";
    }

    // ════════════════════════════════════════════════════════
    // Record interne : Informations d'erreur
    // ════════════════════════════════════════════════════════
    private record ErrorInfo(
            String code,
            String message,
            Map<String, Object> details
    ) {}
}