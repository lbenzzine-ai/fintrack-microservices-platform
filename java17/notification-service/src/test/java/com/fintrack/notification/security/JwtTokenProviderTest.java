package com.fintrack.notification.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private static final String SECRET_BASE64 =
            Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());

    private JwtTokenProvider provider;
    private SecretKey key;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "secretBase64", SECRET_BASE64);
        ReflectionTestUtils.setField(provider, "issuer", "fintrack");
        ReflectionTestUtils.invokeMethod(provider, "init");
        this.key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET_BASE64));
    }

    private String makeToken(String issuer, String subject, long ttlMillis) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .issuer(issuer)
                .subject(subject)
                .issuedAt(new Date(now))
                .expiration(new Date(now + ttlMillis))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    @Test
    void parse_validToken_returnsClaims() {
        String token = makeToken("fintrack", "user-1", 60_000);

        Optional<Claims> claims = provider.parse(token);

        assertThat(claims).isPresent();
        assertThat(claims.get().getSubject()).isEqualTo("user-1");
        assertThat(claims.get().getIssuer()).isEqualTo("fintrack");
    }

    @Test
    void parse_wrongIssuer_returnsEmpty() {
        String token = makeToken("evil", "user-1", 60_000);

        assertThat(provider.parse(token)).isEmpty();
    }

    @Test
    void parse_expiredToken_returnsEmpty() {
        String token = makeToken("fintrack", "user-1", -1_000);

        assertThat(provider.parse(token)).isEmpty();
    }

    @Test
    void parse_malformed_returnsEmpty() {
        assertThat(provider.parse("not-a-jwt")).isEmpty();
    }

    @Test
    void parse_wrongSignature_returnsEmpty() {
        SecretKey other = Keys.hmacShaKeyFor("0000000000000000000000000000000000000000".getBytes());
        String token = Jwts.builder()
                .issuer("fintrack").subject("u")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(other, Jwts.SIG.HS256)
                .compact();

        assertThat(provider.parse(token)).isEmpty();
    }

    @ParameterizedTest
    @CsvSource(value = {
            "Bearer abc.def.ghi,abc.def.ghi",
            "bearer abc.def.ghi,abc.def.ghi",
            "BEARER abc.def.ghi,abc.def.ghi",
            "  abc.def.ghi  ,abc.def.ghi",
            "raw-token,raw-token"
    })
    void stripBearer_handlesVariousPrefixes(String input, String expected) {
        assertThat(JwtTokenProvider.stripBearer(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullSource
    void stripBearer_null_returnsNull(String input) {
        assertThat(JwtTokenProvider.stripBearer(input)).isNull();
    }
}
