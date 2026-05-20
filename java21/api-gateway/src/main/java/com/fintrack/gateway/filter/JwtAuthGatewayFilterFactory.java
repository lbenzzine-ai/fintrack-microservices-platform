package com.fintrack.gateway.filter;

import com.fintrack.gateway.jwt.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Gateway filter — validates the Authorization header and propagates user context downstream
 * as {@code X-User-Id} / {@code X-User-Roles} headers so downstream services don't have to
 * re-validate the JWT.
 *
 * <p>Referenced from {@code api-gateway.yml} as the filter name {@code JwtAuth}.
 */
@Slf4j
@Component
public class JwtAuthGatewayFilterFactory
        extends AbstractGatewayFilterFactory<JwtAuthGatewayFilterFactory.Config> {

    private final JwtTokenProvider tokenProvider;

    public JwtAuthGatewayFilterFactory(JwtTokenProvider tokenProvider) {
        super(Config.class);
        this.tokenProvider = tokenProvider;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            String token = JwtTokenProvider.stripBearer(header);

            Optional<Claims> claimsOpt = tokenProvider.parse(token);
            if (claimsOpt.isEmpty()) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                exchange.getResponse().getHeaders().add("WWW-Authenticate", "Bearer error=\"invalid_token\"");
                return exchange.getResponse().setComplete();
            }

            Claims claims = claimsOpt.get();
            String userId = tokenProvider.userId(claims);
            List<String> roles = tokenProvider.roles(claims);

            HttpHeaders newHeaders = new HttpHeaders();
            newHeaders.addAll(request.getHeaders());
            newHeaders.set("X-User-Id", userId);
            newHeaders.set("X-User-Roles", String.join(",", roles));
            ServerHttpRequest mutated = new ServerHttpRequestDecorator(request) {
                @Override
                public HttpHeaders getHeaders() {
                    return newHeaders;
                }
            };

            return chain.filter(exchange.mutate().request(mutated).build());
        };
    }

    @Data
    public static class Config {
        // currently empty — placeholder for future per-route options (required roles, audience, etc.)
    }
}
