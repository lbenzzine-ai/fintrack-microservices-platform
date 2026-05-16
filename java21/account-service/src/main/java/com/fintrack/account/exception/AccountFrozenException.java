package com.fintrack.account.exception;

import org.springframework.http.HttpStatus;

public class AccountFrozenException extends ApiException {
    public AccountFrozenException(String accountUuid) {
        super(HttpStatus.CONFLICT, "ACCOUNT_FROZEN", "Account is not active: " + accountUuid);
    }
}
