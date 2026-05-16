package com.fintrack.transaction.exception;

import org.springframework.http.HttpStatus;

public class TransactionNotFoundException extends ApiException {
    public TransactionNotFoundException(String identifier) {
        super(HttpStatus.NOT_FOUND, "TRANSACTION_NOT_FOUND", "Transaction not found: " + identifier);
    }
}
