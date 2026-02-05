package com.yassine.learningapp.service;

import com.yassine.learningapp.exception.ErrorCode;
import com.yassine.learningapp.exception.InvalidTokenException;
import com.yassine.learningapp.exception.TokenExpiredException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;

@Slf4j
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expirationMs
    ) {
        this.signingKey = buildSigningKey(secret);
        this.expirationMs = expirationMs;
    }

    // -------------------------
    // Public API
    // -------------------------

    /**
     * Extrait le subject (username). Lance InvalidTokenException/TokenExpiredException.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Variante safe: ne lance pas d'exception.
     */
    public Optional<String> extractUsernameSafe(String token) {
        try {
            return Optional.ofNullable(extractUsername(token));
        } catch (RuntimeException e) {
            log.debug("JWT extractUsernameSafe failed: {}", e.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public long getTimeUntilExpirationSeconds(String token) {
        try {
            Date exp = extractExpiration(token);
            long diffMs = exp.getTime() - System.currentTimeMillis();
            return Math.max(0, diffMs / 1000);
        } catch (RuntimeException e) {
            return 0;
        }
    }

    /**
     * Validation strict : lève des exceptions si token invalide/expiré/subject incohérent.
     */
    public boolean validateTokenStrict(String token, UserDetails userDetails) {
        String username = extractUsername(token); // peut lever
        if (!username.equals(userDetails.getUsername())) {
            throw new InvalidTokenException("Token invalide", ErrorCode.INVALID_SUBJECT);
        }

        // force la lecture de l'expiration (et donc la validation complète du parsing)
        Date exp = extractExpiration(token);
        if (exp.before(new Date())) {
            // En pratique, ExpiredJwtException sera déjà levée au parse, mais on garde cette barrière.
            throw new TokenExpiredException("Token expiré", toLocalDateTime(exp));
        }

        return true;
    }

    /**
     * Validation soft : ne lève jamais d'exception.
     */
    public boolean validateTokenSoft(String token, UserDetails userDetails) {
        try {
            return validateTokenStrict(token, userDetails);
        } catch (RuntimeException e) {
            return false;
        }
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", toRoleNames(userDetails.getAuthorities()));
        return createToken(claims, userDetails.getUsername());
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>(extraClaims);
        claims.putIfAbsent("roles", toRoleNames(userDetails.getAuthorities()));
        return createToken(claims, userDetails.getUsername());
    }

    // -------------------------
    // Internal
    // -------------------------

    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date(System.currentTimeMillis());
        Date exp = new Date(System.currentTimeMillis() + expirationMs);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(exp)
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        try {
            Claims claims = parseAllClaims(token);
            return resolver.apply(claims);

        } catch (ExpiredJwtException e) {
            LocalDateTime expiredAt = toLocalDateTime(e.getClaims().getExpiration());
            throw new TokenExpiredException("Token expiré", expiredAt);

        } catch (SignatureException e) {
            throw new InvalidTokenException("Token invalide", ErrorCode.INVALID_SIGNATURE);

        } catch (MalformedJwtException e) {
            throw new InvalidTokenException("Token invalide", ErrorCode.MALFORMED_TOKEN);

        } catch (UnsupportedJwtException e) {
            throw new InvalidTokenException("Token invalide", ErrorCode.UNSUPPORTED_TOKEN);

        } catch (IllegalArgumentException e) {
            // token null/blank, etc.
            throw new InvalidTokenException("Token invalide", ErrorCode.INVALID_TOKEN);

        } catch (Exception e) {
            // pas de détails -> sécurité
            throw new InvalidTokenException("Token invalide", ErrorCode.INVALID_TOKEN);
        }
    }

    private Claims parseAllClaims(String token) {
        // Ici tu peux rajouter un "trim" / check blank si tu veux être strict
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private static SecretKey buildSigningKey(String secret) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private static LocalDateTime toLocalDateTime(Date date) {
        return Instant.ofEpochMilli(date.getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    private static List<String> toRoleNames(Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream().map(GrantedAuthority::getAuthority).toList();
    }
}
