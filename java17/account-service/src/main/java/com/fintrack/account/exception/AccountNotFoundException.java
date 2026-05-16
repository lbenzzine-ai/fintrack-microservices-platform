package com.fintrack.account.exception;

import org.springframework.http.HttpStatus;

public class AccountNotFoundException extends ApiException {
    public AccountNotFoundException(String identifier) {
        super(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Account not found: " + identifier);
    }
}
