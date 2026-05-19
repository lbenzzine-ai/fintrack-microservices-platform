package com.fintrack.gateway.filter;

import com.fintrack.gateway.jwt.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthGatewayFilterFactoryTest {

    private JwtTokenProvider tokenProvider;
    private JwtAuthGatewayFilterFactory factory;
    private GatewayFilter filter;

    @BeforeEach
    void setUp() {
        tokenProvider = mock(JwtTokenProvider.class);
        factory = new JwtAuthGatewayFilterFactory(tokenProvider);
        filter = factory.apply(new JwtAuthGatewayFilterFactory.Config());
    }

    @Test
    void missingAuthorizationHeader_returnsUnauthorized() {
        when(tokenProvider.parse(any())).thenReturn(Optional.empty());
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/x").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange.getResponse().getHeaders().getFirst("WWW-Authenticate"))
                .contains("Bearer error=\"invalid_token\"");
        verify(chain, never()).filter(any());
    }

    @Test
    void invalidToken_returnsUnauthorized() {
        when(tokenProvider.parse(anyString())).thenReturn(Optional.empty());
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/x")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    void validToken_propagatesUserHeadersDownstream() {
        Claims claims = mock(Claims.class);
        when(tokenProvider.parse(anyString())).thenReturn(Optional.of(claims));
        when(tokenProvider.userId(claims)).thenReturn("user-99");
        when(tokenProvider.roles(claims)).thenReturn(List.of("ADMIN", "USER"));

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/x")
                .header(HttpHeaders.AUTHORIZATION, "Bearer good-token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicReference<String> userId = new AtomicReference<>();
        AtomicReference<String> userRoles = new AtomicReference<>();
        GatewayFilterChain chain = ex -> {
            userId.set(ex.getRequest().getHeaders().getFirst("X-User-Id"));
            userRoles.set(ex.getRequest().getHeaders().getFirst("X-User-Roles"));
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(userId.get()).isEqualTo("user-99");
        assertThat(userRoles.get()).isEqualTo("ADMIN,USER");
        // Status should not have been set to UNAUTHORIZED
        assertThat(exchange.getResponse().getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void validToken_noRoles_propagatesEmptyRolesHeader() {
        Claims claims = mock(Claims.class);
        when(tokenProvider.parse(anyString())).thenReturn(Optional.of(claims));
        when(tokenProvider.userId(claims)).thenReturn("user-1");
        when(tokenProvider.roles(claims)).thenReturn(List.of());

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/x")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ok")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicReference<String> userRoles = new AtomicReference<>();
        GatewayFilterChain chain = ex -> {
            userRoles.set(ex.getRequest().getHeaders().getFirst("X-User-Roles"));
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(userRoles.get()).isEqualTo("");
    }
}
