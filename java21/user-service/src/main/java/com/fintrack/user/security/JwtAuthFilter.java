package com.fintrack.user.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Servlet filter — reads {@code Authorization: Bearer ...}, validates the token via
 * {@link JwtTokenProvider}, and populates {@link SecurityContextHolder}. The Gateway has already
 * validated the token, but user-service re-validates so it can also be exercised directly
 * (e.g. internal calls, tests).
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String token = JwtTokenProvider.stripBearer(req.getHeader(HttpHeaders.AUTHORIZATION));
        if (token != null) {
            tokenProvider.parse(token).ifPresent(claims -> populateAuth(claims, req));
        }
        chain.doFilter(req, res);
    }

    private void populateAuth(Claims claims, HttpServletRequest req) {
        String userUuid = claims.getSubject();
        // Java 21 — pattern-matching switch over the raw claim value
        List<String> roles = switch (claims.getOrDefault("roles", List.of())) {
            case List<?> list -> list.stream().map(String::valueOf).toList();
            case String s     -> List.of(s.split(","));
            default           -> List.of();
        };
        var authorities = roles.stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList();

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userUuid, null, authorities);
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
