package com.fintrack.user.exception;

import org.springframework.http.HttpStatus;

public class EmailAlreadyExistsException extends ApiException {
    public EmailAlreadyExistsException(String email) {
        super(HttpStatus.CONFLICT, "USER_EMAIL_EXISTS", "Email already registered: " + email);
    }
}
