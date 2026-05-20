package com.fintrack.user.security;

import com.fintrack.user.entity.Role;
import com.fintrack.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullSource;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    // 32-byte secret base64-encoded (HS256 requires >= 256 bits).
    private static final String SECRET_RAW = "0123456789abcdef0123456789abcdef";
    private static final String SECRET_BASE64 = Base64.getEncoder().encodeToString(SECRET_RAW.getBytes());

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "secretBase64", SECRET_BASE64);
        ReflectionTestUtils.setField(provider, "issuer", "fintrack");
        ReflectionTestUtils.setField(provider, "accessTtlMinutes", 30L);
        ReflectionTestUtils.setField(provider, "refreshTtlDays", 7L);
        ReflectionTestUtils.invokeMethod(provider, "init");
    }

    private User user() {
        return User.builder()
                .uuid("user-uuid-1")
                .username("alice")
                .roles(Set.of(
                        Role.builder().id(1L).name("USER").build(),
                        Role.builder().id(2L).name("ADMIN").build()))
                .build();
    }

    @Test
    void issueAccessToken_returnsParseableJwt_withExpectedClaims() {
        JwtTokenProvider.IssuedToken issued = provider.issueAccessToken(user());

        assertThat(issued.token()).isNotBlank();
        assertThat(issued.expiresAt()).isAfter(Instant.now());

        Optional<Claims> claims = provider.parse(issued.token());
        assertThat(claims).isPresent();
        Claims c = claims.get();
        assertThat(c.getSubject()).isEqualTo("user-uuid-1");
        assertThat(c.getIssuer()).isEqualTo("fintrack");
        assertThat(c.get("username", String.class)).isEqualTo("alice");
        assertThat(c.get("type", String.class)).isEqualTo("access");
        @SuppressWarnings("unchecked")
        List<String> roles = c.get("roles", List.class);
        assertThat(roles).containsExactlyInAnyOrder("USER", "ADMIN");
    }

    @Test
    void issueRefreshToken_typeClaimIsRefresh_andExpiryLater() {
        JwtTokenProvider.IssuedToken access = provider.issueAccessToken(user());
        JwtTokenProvider.IssuedToken refresh = provider.issueRefreshToken(user());

        assertThat(refresh.expiresAt()).isAfter(access.expiresAt());
        Claims c = provider.parse(refresh.token()).orElseThrow();
        assertThat(c.get("type", String.class)).isEqualTo("refresh");
    }

    @Test
    void parse_validToken_returnsClaims() {
        String token = provider.issueAccessToken(user()).token();
        assertThat(provider.parse(token)).isPresent();
    }

    @Test
    void parse_tamperedSignature_returnsEmpty() {
        String token = provider.issueAccessToken(user()).token();
        // flip a few chars in the signature segment (last segment)
        int lastDot = token.lastIndexOf('.');
        String tampered = token.substring(0, lastDot + 1)
                + (token.charAt(lastDot + 1) == 'A' ? "B" : "A")
                + token.substring(lastDot + 2);
        assertThat(provider.parse(tampered)).isEmpty();
    }

    @Test
    void parse_expiredToken_returnsEmpty() {
        // Manually forge a token that's already expired, signed with the same key.
        SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET_BASE64));
        String expired = Jwts.builder()
                .issuer("fintrack")
                .subject("user-uuid-1")
                .claim("type", "access")
                .issuedAt(Date.from(Instant.now().minusSeconds(3600)))
                .expiration(Date.from(Instant.now().minusSeconds(60)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
        assertThat(provider.parse(expired)).isEmpty();
    }

    @Test
    void parse_wrongIssuer_returnsEmpty() {
        SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET_BASE64));
        String other = Jwts.builder()
                .issuer("other-issuer")
                .subject("user-uuid-1")
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
        assertThat(provider.parse(other)).isEmpty();
    }

    @Test
    void parse_garbageString_returnsEmpty() {
        assertThat(provider.parse("not.a.jwt")).isEmpty();
        assertThat(provider.parse("")).isEmpty();
    }

    @Test
    void stripBearer_withBearerPrefix_stripsAndTrims() {
        assertThat(JwtTokenProvider.stripBearer("Bearer abc.def.ghi")).isEqualTo("abc.def.ghi");
        assertThat(JwtTokenProvider.stripBearer("bearer abc.def.ghi")).isEqualTo("abc.def.ghi");
        assertThat(JwtTokenProvider.stripBearer("BEARER abc.def.ghi")).isEqualTo("abc.def.ghi");
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc.def.ghi", "   abc.def.ghi  "})
    void stripBearer_withoutBearerPrefix_returnsTrimmed(String input) {
        assertThat(JwtTokenProvider.stripBearer(input)).isEqualTo("abc.def.ghi");
    }

    @ParameterizedTest
    @NullSource
    void stripBearer_nullInput_returnsNull(String input) {
        assertThat(JwtTokenProvider.stripBearer(input)).isNull();
    }
}
