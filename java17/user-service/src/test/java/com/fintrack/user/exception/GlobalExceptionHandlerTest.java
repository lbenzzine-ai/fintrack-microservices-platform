package com.fintrack.user.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.core.MethodParameter;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/users/me");
    }

    @Test
    void handleApi_mapsStatusCodeMessageAndPath() {
        ApiException ex = new EmailAlreadyExistsException("a@b.com");

        ResponseEntity<ApiError> resp = handler.handleApi(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        ApiError body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(409);
        assertThat(body.getCode()).isEqualTo("USER_EMAIL_EXISTS");
        assertThat(body.getMessage()).contains("a@b.com");
        assertThat(body.getPath()).isEqualTo("/api/users/me");
        assertThat(body.getTimestamp()).isNotNull();
    }

    @Test
    void handleApi_invalidCredentials_mapsTo401() {
        ResponseEntity<ApiError> resp = handler.handleApi(new InvalidCredentialsException(), request);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resp.getBody().getCode()).isEqualTo("AUTH_INVALID_CREDENTIALS");
    }

    @Test
    void handleApi_userNotFound_mapsTo404() {
        ResponseEntity<ApiError> resp = handler.handleApi(new UserNotFoundException("uuid-x"), request);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody().getCode()).isEqualTo("USER_NOT_FOUND");
        assertThat(resp.getBody().getMessage()).contains("uuid-x");
    }

    @Test
    void handleApi_usernameExists_mapsTo409() {
        ResponseEntity<ApiError> resp = handler.handleApi(new UsernameAlreadyExistsException("alice"), request);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resp.getBody().getCode()).isEqualTo("USER_USERNAME_EXISTS");
    }

    @Test
    void handleValidation_returnsBadRequestWithFieldViolations() throws Exception {
        BindingResult br = mock(BindingResult.class);
        when(br.getFieldErrors()).thenReturn(List.of(
                new FieldError("obj", "username", "must not be blank"),
                new FieldError("obj", "email", "must be a well-formed email")
        ));
        Method m = Dummy.class.getDeclaredMethod("m", String.class);
        MethodParameter mp = new MethodParameter(m, 0);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(mp, br);

        ResponseEntity<ApiError> resp = handler.handleValidation(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ApiError body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getCode()).isEqualTo("VALIDATION_FAILED");
        assertThat(body.getViolations()).hasSize(2)
                .extracting(ApiError.FieldViolation::getField)
                .containsExactlyInAnyOrder("username", "email");
    }

    @Test
    void handleAny_returnsInternalServerErrorWithGenericMessage() {
        ResponseEntity<ApiError> resp = handler.handleAny(new RuntimeException("boom"), request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ApiError body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getCode()).isEqualTo("INTERNAL_ERROR");
        assertThat(body.getMessage()).doesNotContain("boom"); // generic, not leaking internals
        assertThat(body.getPath()).isEqualTo("/api/users/me");
    }

    @SuppressWarnings("unused")
    private static class Dummy {
        void m(String s) {}
    }
}
