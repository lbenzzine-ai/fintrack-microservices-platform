package com.fintrack.user.exception;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends ApiException {
    public UserNotFoundException(String identifier) {
        super(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found: " + identifier);
    }
}
