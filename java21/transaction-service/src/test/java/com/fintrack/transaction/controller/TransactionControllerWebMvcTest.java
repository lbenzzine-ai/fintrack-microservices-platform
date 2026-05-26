package com.fintrack.transaction.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.transaction.dto.CreateTransactionRequest;
import com.fintrack.transaction.dto.FeeQuote;
import com.fintrack.transaction.dto.TransactionResponse;
import com.fintrack.transaction.entity.TransactionType;
import com.fintrack.transaction.exception.TransactionNotFoundException;
import com.fintrack.transaction.security.JwtAuthFilter;
import com.fintrack.transaction.security.JwtTokenProvider;
import com.fintrack.transaction.service.FeeService;
import com.fintrack.transaction.service.TransactionService;
import com.fintrack.transaction.strategy.fee.FeeCalculationContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
@org.springframework.test.context.TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.config.import-check.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "eureka.client.enabled=false"
})
class TransactionControllerWebMvcTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean TransactionService transactionService;
    @MockBean FeeService feeService;
    @MockBean JwtAuthFilter jwtAuthFilter;
    @MockBean JwtTokenProvider jwtTokenProvider;

    private CreateTransactionRequest validRequest() {
        return new CreateTransactionRequest(
                "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                new BigDecimal("100"), "USD",
                TransactionType.DOMESTIC_TRANSFER, "test");
    }

    private static TransactionResponse anyResponse(String uuid) {
        return new TransactionResponse(uuid, null, null, new BigDecimal("100"), null, null, null, null,null, null, null, null, null);
    }

    @Test
    void shouldReturn201CreatedOnValidCreateRequest() throws Exception {
        when(transactionService.create(any())).thenReturn(anyResponse("tx-1"));

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uuid").value("tx-1"))
                .andExpect(jsonPath("$.amount").value(100));
    }

    @Test
    void shouldRejectCreateWithMissingAmount() throws Exception {
        // Records don't allow null amount through Jackson with @NotNull validation if validator runs.
        // GlobalExceptionHandler catches the validation exception → 500.
        CreateTransactionRequest req = new CreateTransactionRequest(
                "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                null, "USD", TransactionType.DOMESTIC_TRANSFER, "test");

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void shouldRejectCreateWithMissingFromAccount() throws Exception {
        CreateTransactionRequest req = new CreateTransactionRequest(
                null,
                "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                new BigDecimal("100"), "USD",
                TransactionType.DOMESTIC_TRANSFER, "test");

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void shouldReturn200WithTransactionWhenFound() throws Exception {
        when(transactionService.findByUuid("tx-1")).thenReturn(anyResponse("tx-1"));

        mockMvc.perform(get("/api/v1/transactions/tx-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value("tx-1"));
    }

    @Test
    void shouldReturn404WhenTransactionNotFound() throws Exception {
        when(transactionService.findByUuid("ghost"))
                .thenThrow(new TransactionNotFoundException("ghost"));

        mockMvc.perform(get("/api/v1/transactions/ghost"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TRANSACTION_NOT_FOUND"));
    }

    @Test
    void shouldReturn200WithListOfTransactionsByAccount() throws Exception {
        Page<TransactionResponse> page = new PageImpl<>(List.of(anyResponse("tx-1"), anyResponse("tx-2")));
        when(transactionService.findByAccount(eq("acc-1"), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/transactions/by-account/acc-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void shouldReturn200WithEmptyArrayWhenNoTransactionsForAccount() throws Exception {
        Page<TransactionResponse> empty = new PageImpl<>(List.of());
        when(transactionService.findByAccount(eq("acc-empty"), any())).thenReturn(empty);

        mockMvc.perform(get("/api/v1/transactions/by-account/acc-empty"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void shouldReturn200WithFeeQuoteOnValidParams() throws Exception {
        FeeQuote q = new FeeQuote("domestic",
                new BigDecimal("100"), new BigDecimal("0.50"), new BigDecimal("100.50"));
        when(feeService.quote(any(FeeCalculationContext.class))).thenReturn(q);

        mockMvc.perform(get("/api/v1/transactions/fee/quote")
                        .param("type", "DOMESTIC_TRANSFER")
                        .param("amount", "100")
                        .param("currencyCode", "USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strategy").value("domestic"))
                .andExpect(jsonPath("$.fee").value(0.50));
    }

    @Test
    void shouldRejectQuoteWhenAmountMissing() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/fee/quote")
                        .param("type", "DOMESTIC_TRANSFER"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void shouldRejectQuoteWhenTypeMissing() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/fee/quote")
                        .param("amount", "100"))
                .andExpect(status().is5xxServerError());
    }
}
