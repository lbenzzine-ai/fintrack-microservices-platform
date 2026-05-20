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
        assertThat(body.getService()).isEqualTo("user-service");
        assertThat(body.getMessage()).contains("User service");
        assertThat(body.getHint()).contains("Circuit breaker");
        assertThat(body.getStatus()).isEqualTo(503);
        assertThat(body.getTimestamp()).isNotNull();
        assertThat(body.getCorrelationId()).isEqualTo("cid-123");
    }

    @Test
    void accounts_returnsServiceUnavailable() {
        ResponseEntity<FallbackResponse> response = controller.accounts("cid-acct");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().getService()).isEqualTo("account-service");
        assertThat(response.getBody().getCorrelationId()).isEqualTo("cid-acct");
    }

    @Test
    void transactions_returnsServiceUnavailableWithRetryHint() {
        ResponseEntity<FallbackResponse> response = controller.transactions("cid-tx");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().getService()).isEqualTo("transaction-service");
        assertThat(response.getBody().getMessage()).contains("NOT recorded");
    }

    @Test
    void notifications_returnsServiceUnavailable() {
        ResponseEntity<FallbackResponse> response = controller.notifications(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().getService()).isEqualTo("notification-service");
        assertThat(response.getBody().getCorrelationId()).isNull();
    }
}
