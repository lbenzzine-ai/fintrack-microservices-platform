package com.fintrack.transaction.controller;

import com.fintrack.transaction.dto.CreateTransactionRequest;
import com.fintrack.transaction.dto.FeeQuote;
import com.fintrack.transaction.dto.TransactionResponse;
import com.fintrack.transaction.entity.TransactionType;
import com.fintrack.transaction.service.FeeService;
import com.fintrack.transaction.service.TransactionService;
import com.fintrack.transaction.strategy.fee.FeeCalculationContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Tag(name = "Transactions", description = "Transfers, fees, saga status")
@SecurityRequirement(name = "bearer-jwt")
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final FeeService feeService;

    @Operation(summary = "Initiate a transaction — starts the saga")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse create(@Valid @RequestBody CreateTransactionRequest req) {
        return transactionService.create(req);
    }

    @Operation(summary = "Get a transaction by its UUID")
    @GetMapping("/{uuid}")
    public TransactionResponse get(@PathVariable String uuid) {
        return transactionService.findByUuid(uuid);
    }

    @Operation(summary = "List transactions where the account is either source or destination")
    @GetMapping("/by-account/{accountUuid}")
    public Page<TransactionResponse> byAccount(@PathVariable String accountUuid, Pageable pageable) {
        return transactionService.findByAccount(accountUuid, pageable);
    }

    @Operation(summary = "Quote fee without creating the transaction")
    @GetMapping("/fee/quote")
    public FeeQuote quote(@RequestParam TransactionType type,
                          @RequestParam BigDecimal amount,
                          @RequestParam(defaultValue = "USD") String currencyCode) {
        return feeService.quote(new FeeCalculationContext(type, amount, currencyCode, null, null, false, false));
    }
}
