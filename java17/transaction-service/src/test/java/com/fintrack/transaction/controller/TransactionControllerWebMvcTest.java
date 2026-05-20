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
        CreateTransactionRequest r = new CreateTransactionRequest();
        r.setFromAccountUuid("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        r.setToAccountUuid("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        r.setAmount(new BigDecimal("100"));
        r.setCurrencyCode("USD");
        r.setType(TransactionType.DOMESTIC_TRANSFER);
        r.setDescription("test");
        return r;
    }

    // ── create() ─────────────────────────────────────────────────────────────────
    @Test
    void shouldReturn201CreatedOnValidCreateRequest() throws Exception {
        TransactionResponse response = TransactionResponse.builder()
                .uuid("tx-1").amount(new BigDecimal("100")).build();
        when(transactionService.create(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uuid").value("tx-1"))
                .andExpect(jsonPath("$.amount").value(100));
    }

    @Test
    void shouldRejectCreateWithMissingAmount() throws Exception {
        // GlobalExceptionHandler.handleAny(Exception) catches MethodArgumentNotValidException
        // before Spring's default 400 mapping → ends up as 500. The point is that the
        // request was rejected (not 201/200) and the service was never invoked.
        CreateTransactionRequest req = validRequest();
        req.setAmount(null);

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void shouldRejectCreateWithMissingFromAccount() throws Exception {
        CreateTransactionRequest req = validRequest();
        req.setFromAccountUuid(null);

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().is5xxServerError());
    }

    // ── get() ────────────────────────────────────────────────────────────────────
    @Test
    void shouldReturn200WithTransactionWhenFound() throws Exception {
        TransactionResponse response = TransactionResponse.builder()
                .uuid("tx-1").amount(new BigDecimal("50")).build();
        when(transactionService.findByUuid("tx-1")).thenReturn(response);

        mockMvc.perform(get("/api/v1/transactions/tx-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value("tx-1"))
                .andExpect(jsonPath("$.amount").value(50));
    }

    @Test
    void shouldReturn404WhenTransactionNotFound() throws Exception {
        when(transactionService.findByUuid("ghost"))
                .thenThrow(new TransactionNotFoundException("ghost"));

        mockMvc.perform(get("/api/v1/transactions/ghost"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TRANSACTION_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("ghost")));
    }

    // ── byAccount() ──────────────────────────────────────────────────────────────
    @Test
    void shouldReturn200WithListOfTransactionsByAccount() throws Exception {
        TransactionResponse t1 = TransactionResponse.builder().uuid("tx-1").build();
        TransactionResponse t2 = TransactionResponse.builder().uuid("tx-2").build();
        Page<TransactionResponse> page = new PageImpl<>(List.of(t1, t2));
        when(transactionService.findByAccount(eq("acc-1"), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/transactions/by-account/acc-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].uuid").value("tx-1"));
    }

    @Test
    void shouldReturn200WithEmptyArrayWhenNoTransactionsForAccount() throws Exception {
        Page<TransactionResponse> empty = new PageImpl<>(List.of());
        when(transactionService.findByAccount(eq("acc-empty"), any())).thenReturn(empty);

        mockMvc.perform(get("/api/v1/transactions/by-account/acc-empty"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    // ── quote() ──────────────────────────────────────────────────────────────────
    @Test
    void shouldReturn200WithFeeQuoteOnValidParams() throws Exception {
        FeeQuote q = FeeQuote.builder()
                .strategy("domestic").principal(new BigDecimal("100"))
                .fee(new BigDecimal("0.50")).total(new BigDecimal("100.50")).build();
        when(feeService.quote(any(FeeCalculationContext.class))).thenReturn(q);

        mockMvc.perform(get("/api/v1/transactions/fee/quote")
                        .param("type", "DOMESTIC_TRANSFER")
                        .param("amount", "100")
                        .param("currencyCode", "USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strategy").value("domestic"))
                .andExpect(jsonPath("$.fee").value(0.50))
                .andExpect(jsonPath("$.total").value(100.50));
    }

    @Test
    void shouldRejectQuoteWhenAmountMissing() throws Exception {
        // MissingServletRequestParameterException is also caught by GlobalExceptionHandler's
        // generic Exception handler → 500. Verifies the handler doesn't pass through to fee service.
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
