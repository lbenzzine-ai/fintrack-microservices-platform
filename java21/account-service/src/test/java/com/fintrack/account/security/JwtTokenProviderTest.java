package com.fintrack.account.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private static final String SECRET_BASE64 =
            Base64.getEncoder().encodeToString(new byte[]{
                    1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,
                    17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32
            });

    private static final String ISSUER = "fintrack";

    private JwtTokenProvider provider;
    private SecretKey key;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "secretBase64", SECRET_BASE64);
        ReflectionTestUtils.setField(provider, "issuer", ISSUER);
        ReflectionTestUtils.invokeMethod(provider, "init");
        key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET_BASE64));
    }

    private String tokenWith(String issuer, long ttlMillis, String subject, List<String> roles) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + ttlMillis);
        return Jwts.builder()
                .issuer(issuer)
                .subject(subject)
                .issuedAt(now)
                .expiration(exp)
                .claims(Map.of("roles", roles, "userId", subject))
                .signWith(key)
                .compact();
    }

    @Test
    void parse_valid_token_returns_claims_with_subject_and_custom_claims() {
        String token = tokenWith(ISSUER, 60_000, "user-123", List.of("USER", "ADMIN"));

        Optional<Claims> parsed = provider.parse(token);

        assertThat(parsed).isPresent();
        Claims c = parsed.get();
        assertThat(c.getSubject()).isEqualTo("user-123");
        assertThat(c.getIssuer()).isEqualTo(ISSUER);
        assertThat(c.get("userId", String.class)).isEqualTo("user-123");
        assertThat(c.get("roles", List.class)).containsExactly("USER", "ADMIN");
        assertThat(c.getExpiration()).isAfter(new Date());
    }

    @Test
    void parse_expired_token_returns_empty() {
        String token = tokenWith(ISSUER, -10_000, "u", List.of("USER"));
        assertThat(provider.parse(token)).isEmpty();
    }

    @Test
    void parse_wrong_issuer_returns_empty() {
        String token = tokenWith("other-issuer", 60_000, "u", List.of("USER"));
        assertThat(provider.parse(token)).isEmpty();
    }

    @Test
    void parse_tampered_signature_returns_empty() {
        String token = tokenWith(ISSUER, 60_000, "u", List.of("USER"));
        char last = token.charAt(token.length() - 1);
        char swap = last == 'A' ? 'B' : 'A';
        String tampered = token.substring(0, token.length() - 1) + swap;
        assertThat(provider.parse(tampered)).isEmpty();
    }

    @Test
    void parse_signed_with_different_key_returns_empty() {
        byte[] otherSecret = new byte[32];
        for (int i = 0; i < 32; i++) otherSecret[i] = (byte) (32 - i);
        SecretKey otherKey = Keys.hmacShaKeyFor(otherSecret);
        String token = Jwts.builder()
                .issuer(ISSUER).subject("u")
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(otherKey).compact();

        assertThat(provider.parse(token)).isEmpty();
    }

    @Test
    void parse_garbage_returns_empty() {
        assertThat(provider.parse("not-a-jwt")).isEmpty();
    }

    @Test
    void parse_null_returns_empty() {
        assertThat(provider.parse(null)).isEmpty();
    }

    @Test
    void stripBearer_removes_prefix_case_insensitively() {
        assertThat(JwtTokenProvider.stripBearer("Bearer abc.def.ghi")).isEqualTo("abc.def.ghi");
        assertThat(JwtTokenProvider.stripBearer("bearer abc.def.ghi")).isEqualTo("abc.def.ghi");
        assertThat(JwtTokenProvider.stripBearer("BEARER abc.def.ghi")).isEqualTo("abc.def.ghi");
    }

    @Test
    void stripBearer_trims_whitespace() {
        assertThat(JwtTokenProvider.stripBearer("Bearer   token-x   ")).isEqualTo("token-x");
        assertThat(JwtTokenProvider.stripBearer("  raw-token  ")).isEqualTo("raw-token");
    }

    @Test
    void stripBearer_null_returns_null() {
        assertThat(JwtTokenProvider.stripBearer(null)).isNull();
    }

    @Test
    void stripBearer_no_prefix_returns_trimmed_input() {
        assertThat(JwtTokenProvider.stripBearer("xyz")).isEqualTo("xyz");
    }
}
