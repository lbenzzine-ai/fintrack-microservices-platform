package com.fintrack.account.controller;

import com.fintrack.account.dto.AccountResponse;
import com.fintrack.account.dto.BalanceResponse;
import com.fintrack.account.dto.CreateAccountRequest;
import com.fintrack.account.dto.InterestPreview;
import com.fintrack.account.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@Tag(name = "Accounts", description = "User wallets, balances and interest previews")
@SecurityRequirement(name = "bearer-jwt")
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @Operation(summary = "Open a wallet for the authenticated user (idempotent on userUuid)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse open(@AuthenticationPrincipal String userUuid,
                                @Valid @RequestBody CreateAccountRequest req) {
        return accountService.createForUser(userUuid, req.getCurrencyCode());
    }

    @Operation(summary = "Get an account by its UUID")
    @GetMapping("/{uuid}")
    public AccountResponse get(@PathVariable String uuid) {
        return accountService.findByUuid(uuid);
    }

    @Operation(summary = "Get the wallet of the authenticated user")
    @GetMapping("/me")
    public AccountResponse mine(@AuthenticationPrincipal String userUuid){
        return accountService.findByUserUuid(userUuid);
    }

    @Operation(summary = "Balance only — cached aggressively")
    @GetMapping("/{uuid}/balance")
    public BalanceResponse balance(@PathVariable String uuid) {
        return accountService.balance(uuid);
    }

    @Operation(summary = "Preview interest using a chosen Strategy (defaults to fintrack.account.interest.strategy)")
    @GetMapping("/{uuid}/interest/preview")
    public InterestPreview interestPreview(@PathVariable String uuid,
                                           @RequestParam(defaultValue = "0.025") BigDecimal annualRate,
                                           @RequestParam(defaultValue = "12") int months,
                                           @RequestParam(required = false) String strategy) {
        return accountService.previewInterest(uuid, annualRate, months, strategy);
    }
}
