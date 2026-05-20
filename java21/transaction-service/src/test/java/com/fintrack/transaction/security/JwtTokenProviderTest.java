package com.fintrack.transaction.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import javax.crypto.SecretKey;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private static final String SECRET_BASE64 =
            Base64.getEncoder().encodeToString("test-secret-needs-32-bytes-minimum!".getBytes());
    private static final String ISSUER = "fintrack-test";

    private SecretKey key;
    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() throws Exception {
        provider = new JwtTokenProvider();
        setField(provider, "secretBase64", SECRET_BASE64);
        setField(provider, "issuer", ISSUER);
        java.lang.reflect.Method init = JwtTokenProvider.class.getDeclaredMethod("init");
        init.setAccessible(true);
        init.invoke(provider);
        key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET_BASE64));
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    private String validToken() {
        return Jwts.builder()
                .issuer(ISSUER)
                .subject("user-1")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(600)))
                .signWith(key)
                .compact();
    }

    @Test
    void parse_returnsClaimsForValidToken() {
        Optional<Claims> out = provider.parse(validToken());
        assertThat(out).isPresent();
        assertThat(out.get().getSubject()).isEqualTo("user-1");
    }

    @Test
    void parse_returnsEmptyForExpiredToken() {
        String expired = Jwts.builder()
                .issuer(ISSUER)
                .subject("user-1")
                .issuedAt(Date.from(Instant.now().minusSeconds(7200)))
                .expiration(Date.from(Instant.now().minusSeconds(3600)))
                .signWith(key)
                .compact();
        assertThat(provider.parse(expired)).isEmpty();
    }

    @Test
    void parse_returnsEmptyForWrongIssuer() {
        String wrongIssuer = Jwts.builder()
                .issuer("other-issuer")
                .subject("user-1")
                .expiration(Date.from(Instant.now().plusSeconds(600)))
                .signWith(key)
                .compact();
        assertThat(provider.parse(wrongIssuer)).isEmpty();
    }

    @Test
    void parse_tamperedSignature_returnsEmpty() {
        String token = Jwts.builder()
                .issuer(ISSUER)
                .subject("user-1")
                .expiration(Date.from(Instant.now().plusSeconds(600)))
                .signWith(key)
                .compact();
        int lastDot = token.lastIndexOf('.');
        String tampered = token.substring(0, lastDot + 1)
                + "invalidsignatureXXXXXXXXXXXXXXXX";
        assertThat(provider.parse(tampered)).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"not-a-jwt", "garbage.token.value"})
    void parse_returnsEmptyForMalformed(String token) {
        assertThat(provider.parse(token)).isEmpty();
    }

    @Test
    void stripBearer_removesPrefixCaseInsensitive() {
        assertThat(JwtTokenProvider.stripBearer("Bearer abc")).isEqualTo("abc");
        assertThat(JwtTokenProvider.stripBearer("bearer abc")).isEqualTo("abc");
        assertThat(JwtTokenProvider.stripBearer("BEARER  abc  ")).isEqualTo("abc");
    }

    @Test
    void stripBearer_passesThroughWhenNoPrefix() {
        assertThat(JwtTokenProvider.stripBearer("abc")).isEqualTo("abc");
        assertThat(JwtTokenProvider.stripBearer("  raw-token  ")).isEqualTo("raw-token");
    }

    @Test
    void stripBearer_handlesNull() {
        assertThat(JwtTokenProvider.stripBearer(null)).isNull();
    }
}
