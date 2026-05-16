package com.fintrack.gateway.controller;

import com.fintrack.gateway.dto.FallbackResponse;
import com.fintrack.gateway.filter.CorrelationIdGlobalFilter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Fallback responses for Resilience4j circuit breakers. The gateway's per-route
 * {@code CircuitBreaker} filter forwards to {@code /fallback/<service>} when the
 * downstream call is short-circuited.
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/users")
    @PostMapping("/users")
    public ResponseEntity<FallbackResponse> users(
            @RequestHeader(value = CorrelationIdGlobalFilter.HEADER, required = false) String cid) {
        return build("user-service", "User service is temporarily unavailable.", cid);
    }

    @GetMapping("/accounts")
    @PostMapping("/accounts")
    public ResponseEntity<FallbackResponse> accounts(
            @RequestHeader(value = CorrelationIdGlobalFilter.HEADER, required = false) String cid) {
        return build("account-service", "Account service is temporarily unavailable.", cid);
    }

    @GetMapping("/transactions")
    @PostMapping("/transactions")
    public ResponseEntity<FallbackResponse> transactions(
            @RequestHeader(value = CorrelationIdGlobalFilter.HEADER, required = false) String cid) {
        return build("transaction-service",
                "Transaction service is temporarily unavailable. Your request was NOT recorded — please retry.", cid);
    }

    @GetMapping("/notifications")
    @PostMapping("/notifications")
    public ResponseEntity<FallbackResponse> notifications(
            @RequestHeader(value = CorrelationIdGlobalFilter.HEADER, required = false) String cid) {
        return build("notification-service", "Notification service is temporarily unavailable.", cid);
    }

    private ResponseEntity<FallbackResponse> build(String service, String message, String cid) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(FallbackResponse.builder()
                        .service(service)
                        .message(message)
                        .hint("Circuit breaker is OPEN. Retry after the breaker transitions back to HALF_OPEN/CLOSED.")
                        .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                        .timestamp(Instant.now())
                        .correlationId(cid)
                        .build());
    }
}
