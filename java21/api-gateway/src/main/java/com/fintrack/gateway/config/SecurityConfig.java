package com.fintrack.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Reactive (WebFlux) security for the gateway.
 *
 * <p>All real authn/authz is enforced per-route by the {@code JwtAuth} gateway filter
 * (declared in {@code api-gateway.yml}); Spring Security here only:
 *  <ul>
 *      <li>permits unauthenticated access to anonymous endpoints (auth, swagger, actuator probes, fallbacks)</li>
 *      <li>relies on the upstream filter for everything else</li>
 *  </ul>
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(ex -> ex
                        // public endpoints
                        .pathMatchers(HttpMethod.POST, "/api/v1/auth/**").permitAll()
                        .pathMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**",
                                      "/webjars/**", "/aggregate/**").permitAll()
                        .pathMatchers("/fallback/**").permitAll()
                        .pathMatchers("/actuator/health/**", "/actuator/info",
                                      "/actuator/prometheus", "/actuator/gateway/**").permitAll()
                        // everything else: the JwtAuth filter decides per-route
                        .anyExchange().permitAll())
                .build();
    }
}
