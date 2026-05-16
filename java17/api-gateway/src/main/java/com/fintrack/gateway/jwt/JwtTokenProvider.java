package com.fintrack.gateway.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * Validates HS256 JWTs issued by user-service.
 * <p>
 * The secret is read from {@code fintrack.security.jwt.secret} (base64 of a 256-bit key) and
 * shared with user-service via Spring Cloud Config. The token's {@code sub} claim carries the
 * user id and {@code roles} a comma-separated string of authorities.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    @Value("${fintrack.security.jwt.secret}")
    private String secretBase64;

    @Value("${fintrack.security.jwt.issuer:fintrack}")
    private String expectedIssuer;

    private SecretKey signingKey;

    @PostConstruct
    void init() {
        byte[] decoded = Base64.getDecoder().decode(secretBase64);
        this.signingKey = Keys.hmacShaKeyFor(decoded);
        log.info("JwtTokenProvider initialized — expected issuer='{}'", expectedIssuer);
    }

    public Optional<Claims> parse(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(expectedIssuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("JWT validation failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public String userId(Claims claims) {
        return claims.getSubject();
    }

    public List<String> roles(Claims claims) {
        Object raw = claims.get("roles");
        if (raw == null) return List.of();
        if (raw instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of(String.valueOf(raw).split(","));
    }

    public static String stripBearer(String header) {
        if (header == null) return null;
        return header.regionMatches(true, 0, "Bearer ", 0, 7) ? header.substring(7).trim() : header.trim();
    }
}
