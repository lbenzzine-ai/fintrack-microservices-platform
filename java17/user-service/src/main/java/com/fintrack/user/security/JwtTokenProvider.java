package com.fintrack.user.security;

import com.fintrack.user.entity.Role;
import com.fintrack.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Issues and validates HS256 JWT access + refresh tokens. The secret is shared with
 * api-gateway via Spring Cloud Config under {@code fintrack.security.jwt.secret} so the
 * gateway can verify without calling user-service.
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    @Value("${fintrack.security.jwt.secret}")
    private String secretBase64;

    @Value("${fintrack.security.jwt.issuer:fintrack}")
    private String issuer;

    @Value("${fintrack.security.jwt.access-token-ttl-minutes:30}")
    private long accessTtlMinutes;

    @Value("${fintrack.security.jwt.refresh-token-ttl-days:7}")
    private long refreshTtlDays;

    private SecretKey key;

    @PostConstruct
    void init() {
        this.key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secretBase64));
    }

    public IssuedToken issueAccessToken(User user) {
        return issue(user, TYPE_ACCESS, Duration.ofMinutes(accessTtlMinutes));
    }

    public IssuedToken issueRefreshToken(User user) {
        return issue(user, TYPE_REFRESH, Duration.ofDays(refreshTtlDays));
    }

    private IssuedToken issue(User user, String type, Duration ttl) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(ttl);
        List<String> roles = user.getRoles().stream().map(Role::getName).toList();
        String token = Jwts.builder()
                .issuer(issuer)
                .subject(user.getUuid())
                .claim("username", user.getUsername())
                .claim(CLAIM_ROLES, roles)
                .claim(CLAIM_TYPE, type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
        return new IssuedToken(token, expiresAt);
    }

    public Optional<Claims> parse(String token) {
        try {
            return Optional.of(Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload());
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("JWT parse failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public static String stripBearer(String header) {
        if (header == null) return null;
        return header.regionMatches(true, 0, "Bearer ", 0, 7) ? header.substring(7).trim() : header.trim();
    }

    public record IssuedToken(String token, Instant expiresAt) {}
}
