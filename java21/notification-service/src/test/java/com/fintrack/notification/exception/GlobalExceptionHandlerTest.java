package com.fintrack.notification.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Mock HttpServletRequest req;

    @Test
    void handleApi_mapsStatusCodeAndPath() {
        when(req.getRequestURI()).thenReturn("/notifications/abc");
        ApiException ex = new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "missing");

        ResponseEntity<ApiError> resp = handler.handleApi(ex, req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ApiError body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(404);
        assertThat(body.getCode()).isEqualTo("NOT_FOUND");
        assertThat(body.getMessage()).isEqualTo("missing");
        assertThat(body.getPath()).isEqualTo("/notifications/abc");
        assertThat(body.getTimestamp()).isNotNull();
    }

    @Test
    void handleApi_notFoundException_propagates404() {
        when(req.getRequestURI()).thenReturn("/x");
        NotificationNotFoundException ex = new NotificationNotFoundException("u1");

        ResponseEntity<ApiError> resp = handler.handleApi(ex, req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody().getCode()).isEqualTo("NOTIFICATION_NOT_FOUND");
        assertThat(resp.getBody().getMessage()).contains("u1");
    }

    @Test
    void handleAny_returns500_withGenericMessage() {
        when(req.getRequestURI()).thenReturn("/anything");

        ResponseEntity<ApiError> resp = handler.handleAny(new RuntimeException("boom"), req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resp.getBody().getStatus()).isEqualTo(500);
        assertThat(resp.getBody().getCode()).isEqualTo("INTERNAL_ERROR");
        assertThat(resp.getBody().getMessage()).isEqualTo("Unexpected error — see correlationId in logs");
        assertThat(resp.getBody().getPath()).isEqualTo("/anything");
    }
}
