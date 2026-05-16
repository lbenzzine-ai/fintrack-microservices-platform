package com.fintrack.transaction.exception;

import org.springframework.http.HttpStatus;

public class InvalidTransactionException extends ApiException {
    public InvalidTransactionException(String message) {
        super(HttpStatus.BAD_REQUEST, "INVALID_TRANSACTION", message);
    }
}
