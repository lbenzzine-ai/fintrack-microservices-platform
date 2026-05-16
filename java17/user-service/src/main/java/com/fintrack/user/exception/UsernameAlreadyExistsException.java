package com.fintrack.user.exception;

import org.springframework.http.HttpStatus;

public class UsernameAlreadyExistsException extends ApiException {
    public UsernameAlreadyExistsException(String username) {
        super(HttpStatus.CONFLICT, "USER_USERNAME_EXISTS", "Username already taken: " + username);
    }
}
