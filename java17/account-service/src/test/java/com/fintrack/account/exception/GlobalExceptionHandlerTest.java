package com.fintrack.account.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock HttpServletRequest request;

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleApi_uses_exception_status_code_and_message() {
        when(request.getRequestURI()).thenReturn("/api/accounts/missing");
        ApiException ex = new AccountNotFoundException("missing");

        ResponseEntity<ApiError> resp = handler.handleApi(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ApiError body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(404);
        assertThat(body.getCode()).isEqualTo("ACCOUNT_NOT_FOUND");
        assertThat(body.getMessage()).contains("missing");
        assertThat(body.getPath()).isEqualTo("/api/accounts/missing");
        assertThat(body.getTimestamp()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void handleApi_propagates_frozen_status() {
        when(request.getRequestURI()).thenReturn("/api/accounts/acc/debit");
        ResponseEntity<ApiError> resp = handler.handleApi(new AccountFrozenException("acc"), request);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resp.getBody().getCode()).isEqualTo("ACCOUNT_FROZEN");
    }

    @Test
    void handleApi_propagates_insufficient_funds_status() {
        when(request.getRequestURI()).thenReturn("/api/accounts/acc/debit");
        ResponseEntity<ApiError> resp = handler.handleApi(new InsufficientFundsException("acc"), request);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(resp.getBody().getCode()).isEqualTo("INSUFFICIENT_FUNDS");
    }

    @Test
    void handleAny_maps_unknown_to_500_with_generic_payload() {
        when(request.getRequestURI()).thenReturn("/x");
        Exception ex = new RuntimeException("boom");

        ResponseEntity<ApiError> resp = handler.handleAny(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ApiError body = resp.getBody();
        assertThat(body.getStatus()).isEqualTo(500);
        assertThat(body.getCode()).isEqualTo("INTERNAL_ERROR");
        // Generic message — the original exception's message must NOT leak through.
        assertThat(body.getMessage()).isEqualTo("Unexpected error");
        assertThat(body.getPath()).isEqualTo("/x");
        assertThat(body.getTimestamp()).isNotNull();
    }
}
