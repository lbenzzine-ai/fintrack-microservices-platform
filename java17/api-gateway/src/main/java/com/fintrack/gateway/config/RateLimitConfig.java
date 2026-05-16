package com.fintrack.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

/**
 * KeyResolver beans referenced by {@code RequestRateLimiter} filters in {@code api-gateway.yml}.
 *
 * <ul>
 *   <li>{@code ipKeyResolver}    — anonymous endpoints (e.g. /auth/login) key by remote IP</li>
 *   <li>{@code userKeyResolver}  — authenticated endpoints key by the user id propagated
 *                                  via the {@code X-User-Id} header from {@code JwtAuth}</li>
 * </ul>
 */
@Configuration
public class RateLimitConfig {

    @Bean
    @Primary
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.justOrEmpty(exchange.getRequest().getRemoteAddress())
                .map(addr -> addr.getAddress().getHostAddress())
                .defaultIfEmpty("unknown-ip");
    }

    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            return Mono.just(userId != null ? "user:" + userId
                    : "ip:" + (exchange.getRequest().getRemoteAddress() != null
                            ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                            : "unknown"));
        };
    }
}
