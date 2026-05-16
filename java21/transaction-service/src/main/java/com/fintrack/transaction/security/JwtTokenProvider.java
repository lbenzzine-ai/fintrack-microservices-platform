package com.fintrack.transaction.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Optional;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${fintrack.security.jwt.secret}")
    private String secretBase64;

    @Value("${fintrack.security.jwt.issuer:fintrack}")
    private String issuer;

    private SecretKey key;

    @PostConstruct
    void init() { this.key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secretBase64)); }

    public Optional<Claims> parse(String token) {
        try {
            return Optional.of(Jwts.parser()
                    .verifyWith(key).requireIssuer(issuer).build()
                    .parseSignedClaims(token).getPayload());
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("JWT parse failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public static String stripBearer(String h) {
        if (h == null) return null;
        return h.regionMatches(true, 0, "Bearer ", 0, 7) ? h.substring(7).trim() : h.trim();
    }
}
