package com.fintrack.gateway.controller;

import com.fintrack.gateway.dto.FallbackResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class FallbackControllerTest {

    private final FallbackController controller = new FallbackController();

    @Test
    void users_returnsServiceUnavailableWithCorrelationId() {
        ResponseEntity<FallbackResponse> response = controller.users("cid-123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        FallbackResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.service()).isEqualTo("user-service");
        assertThat(body.message()).contains("User service");
        assertThat(body.hint()).contains("Circuit breaker");
        assertThat(body.status()).isEqualTo(503);
        assertThat(body.timestamp()).isNotNull();
        assertThat(body.correlationId()).isEqualTo("cid-123");
    }

    @Test
    void accounts_returnsServiceUnavailable() {
        ResponseEntity<FallbackResponse> response = controller.accounts("cid-acct");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().service()).isEqualTo("account-service");
        assertThat(response.getBody().correlationId()).isEqualTo("cid-acct");
    }

    @Test
    void transactions_returnsServiceUnavailableWithRetryHint() {
        ResponseEntity<FallbackResponse> response = controller.transactions("cid-tx");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().service()).isEqualTo("transaction-service");
        assertThat(response.getBody().message()).contains("NOT recorded");
    }

    @Test
    void notifications_returnsServiceUnavailable() {
        ResponseEntity<FallbackResponse> response = controller.notifications(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().service()).isEqualTo("notification-service");
        assertThat(response.getBody().correlationId()).isNull();
    }
}
